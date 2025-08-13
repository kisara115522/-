/* 客服聊天组件 - 可嵌入任何页面 */
(function() {
    'use strict';

    // 检查是否已经加载
    if (window.CustomerServiceWidget) {
        return;
    }

    // 客服组件类
    class CustomerServiceWidget {
        constructor(options = {}) {
            this.options = {
                apiUrl: '/chat',
                buttonText: '客服',
                welcomeMessage: '您好！我是智能客服，有什么可以帮助您的吗？',
                position: 'bottom-right', // bottom-right, bottom-left, top-right, top-left
                ...options
            };
            
            this.isOpen = false;
            this.messages = [];
            this.isLoading = false;
            
            this.init();
        }

        init() {
            this.loadStyles();
            this.createElements();
            this.bindEvents();
        }

        loadStyles() {
            if (document.getElementById('customer-service-styles')) return;
            
            const styles = `
                <style id="customer-service-styles">
                /* 客服组件样式 */
                .cs-widget-container * {
                    box-sizing: border-box;
                }
                
                .cs-widget-button {
                    position: fixed;
                    width: 60px;
                    height: 60px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    border-radius: 50%;
                    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
                    cursor: move;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-size: 24px;
                    transition: all 0.3s ease;
                    z-index: 9999;
                    border: none;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    user-select: none;
                }
                
                .cs-widget-button:hover {
                    transform: scale(1.1);
                    box-shadow: 0 6px 25px rgba(102, 126, 234, 0.6);
                }
                
                .cs-widget-button.dragging {
                    cursor: grabbing;
                    transition: none;
                    transform: scale(1.05);
                }
                
                .cs-widget-button.bottom-right {
                    right: 20px;
                    bottom: 20px;
                }
                
                .cs-widget-button.bottom-left {
                    left: 20px;
                    bottom: 20px;
                }
                
                .cs-chat-window {
                    position: fixed;
                    width: 350px;
                    height: 500px;
                    background: white;
                    border-radius: 12px;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                    transition: all 0.3s ease;
                    z-index: 9998;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                }
                
                .cs-chat-window.bottom-right {
                    right: 20px;
                    bottom: 90px;
                }
                
                .cs-chat-window.bottom-left {
                    left: 20px;
                    bottom: 90px;
                }
                
                .cs-chat-window.hidden {
                    transform: scale(0.8) translateY(20px);
                    opacity: 0;
                    pointer-events: none;
                }
                
                .cs-chat-header {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    padding: 15px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                
                .cs-chat-header h3 {
                    margin: 0;
                    font-size: 16px;
                    font-weight: 600;
                }
                
                .cs-chat-header p {
                    margin: 0;
                    font-size: 12px;
                    opacity: 0.9;
                }
                
                .cs-close-btn {
                    background: none;
                    border: none;
                    color: white;
                    cursor: pointer;
                    padding: 5px;
                    border-radius: 4px;
                    transition: background-color 0.2s;
                }
                
                .cs-close-btn:hover {
                    background: rgba(255, 255, 255, 0.2);
                }
                
                .cs-messages-container {
                    flex: 1;
                    overflow-y: auto;
                    padding: 15px;
                    background: #f8fafc;
                }
                
                .cs-messages-container::-webkit-scrollbar {
                    width: 4px;
                }
                
                .cs-messages-container::-webkit-scrollbar-track {
                    background: #f1f1f1;
                }
                
                .cs-messages-container::-webkit-scrollbar-thumb {
                    background: #c1c1c1;
                    border-radius: 2px;
                }
                
                .cs-message {
                    display: flex;
                    margin-bottom: 12px;
                }
                
                .cs-message.user {
                    justify-content: flex-end;
                }
                
                .cs-message-bubble {
                    max-width: 80%;
                    padding: 10px 14px;
                    border-radius: 18px;
                    word-wrap: break-word;
                    font-size: 14px;
                    line-height: 1.4;
                }
                
                .cs-message-bubble.user {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    border-bottom-right-radius: 6px;
                }
                
                .cs-message-bubble.assistant {
                    background: white;
                    color: #374151;
                    border: 1px solid #e5e7eb;
                    border-bottom-left-radius: 6px;
                }
                
                .cs-input-area {
                    padding: 15px;
                    border-top: 1px solid #e5e7eb;
                    background: white;
                }
                
                .cs-input-container {
                    display: flex;
                    align-items: flex-end;
                    gap: 10px;
                }
                
                .cs-input-container textarea {
                    flex: 1;
                    min-height: 40px;
                    max-height: 120px;
                    padding: 10px 14px;
                    border: 1px solid #d1d5db;
                    border-radius: 20px;
                    resize: none;
                    font-size: 14px;
                    outline: none;
                    transition: border-color 0.2s;
                    font-family: inherit;
                }
                
                .cs-input-container textarea:focus {
                    border-color: #667eea;
                }
                
                .cs-send-btn {
                    width: 40px;
                    height: 40px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    border-radius: 50%;
                    border: none;
                    color: white;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    transition: transform 0.2s;
                }
                
                .cs-send-btn:hover:not(:disabled) {
                    transform: scale(1.1);
                }
                
                .cs-send-btn:disabled {
                    opacity: 0.6;
                    cursor: not-allowed;
                }
                
                .cs-loading {
                    display: inline-block;
                }
                
                .cs-loading::after {
                    content: '.';
                    animation: cs-loading-dots 1.4s infinite;
                }
                
                @keyframes cs-loading-dots {
                    0%, 20% { content: '.'; }
                    40% { content: '..'; }
                    60%, 100% { content: '...'; }
                }
                
                @media (max-width: 480px) {
                    .cs-chat-window {
                        right: 10px !important;
                        left: 10px !important;
                        width: auto !important;
                        bottom: 80px !important;
                        height: 400px !important;
                    }
                    
                    .cs-widget-button {
                        right: 15px !important;
                        bottom: 15px !important;
                        width: 50px !important;
                        height: 50px !important;
                        font-size: 20px !important;
                    }
                }
                </style>
            `;
            
            document.head.insertAdjacentHTML('beforeend', styles);
        }

        createElements() {
            // 创建容器
            this.container = document.createElement('div');
            this.container.className = 'cs-widget-container';
            
            // 创建按钮
            this.button = document.createElement('button');
            this.button.className = `cs-widget-button ${this.options.position}`;
            this.button.innerHTML = '💬';
            this.button.title = this.options.buttonText;
            
            // 创建聊天窗口
            this.chatWindow = document.createElement('div');
            this.chatWindow.className = `cs-chat-window ${this.options.position} hidden`;
            
            this.chatWindow.innerHTML = `
                <div class="cs-chat-header">
                    <div>
                        <h3>在线客服</h3>
                        <p>我们随时为您服务</p>
                    </div>
                    <button class="cs-close-btn">✕</button>
                </div>
                <div class="cs-messages-container"></div>
                <div class="cs-input-area">
                    <div class="cs-input-container">
                        <textarea placeholder="请输入您的问题..." maxlength="1000"></textarea>
                        <button class="cs-send-btn">
                            <span>➤</span>
                        </button>
                    </div>
                </div>
            `;
            
            this.container.appendChild(this.button);
            this.container.appendChild(this.chatWindow);
            document.body.appendChild(this.container);
            
            // 获取元素引用
            this.messagesContainer = this.chatWindow.querySelector('.cs-messages-container');
            this.textarea = this.chatWindow.querySelector('textarea');
            this.sendButton = this.chatWindow.querySelector('.cs-send-btn');
            this.closeButton = this.chatWindow.querySelector('.cs-close-btn');
        }

        bindEvents() {
            // 按钮拖拽功能
            this.isDragging = false;
            this.startX = 0;
            this.startY = 0;
            this.initialX = 0;
            this.initialY = 0;
            
            // 鼠标按下开始拖拽
            this.button.addEventListener('mousedown', (e) => {
                this.isDragging = false;
                this.startX = e.clientX;
                this.startY = e.clientY;
                
                const rect = this.button.getBoundingClientRect();
                this.initialX = rect.left;
                this.initialY = rect.top;
                
                this.button.classList.add('dragging');
                
                // 添加全局鼠标移动和释放事件
                document.addEventListener('mousemove', this.handleMouseMove);
                document.addEventListener('mouseup', this.handleMouseUp);
                
                e.preventDefault();
            });
            
            // 鼠标移动处理
            this.handleMouseMove = (e) => {
                const deltaX = e.clientX - this.startX;
                const deltaY = e.clientY - this.startY;
                
                // 如果移动距离超过5px，认为是拖拽
                if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                    this.isDragging = true;
                    
                    let newX = this.initialX + deltaX;
                    let newY = this.initialY + deltaY;
                    
                    // 边界检查
                    const maxX = window.innerWidth - 60;
                    const maxY = window.innerHeight - 60;
                    
                    newX = Math.max(0, Math.min(maxX, newX));
                    newY = Math.max(0, Math.min(maxY, newY));
                    
                    this.button.style.left = newX + 'px';
                    this.button.style.top = newY + 'px';
                    this.button.style.right = 'auto';
                    this.button.style.bottom = 'auto';
                }
            };
            
            // 鼠标释放处理
            this.handleMouseUp = (e) => {
                document.removeEventListener('mousemove', this.handleMouseMove);
                document.removeEventListener('mouseup', this.handleMouseUp);
                
                this.button.classList.remove('dragging');
                
                // 如果没有拖拽，触发点击事件
                if (!this.isDragging) {
                    this.toggleChat();
                }
                
                this.isDragging = false;
            };
            
            // 触摸事件支持（移动端）
            this.button.addEventListener('touchstart', (e) => {
                const touch = e.touches[0];
                this.isDragging = false;
                this.startX = touch.clientX;
                this.startY = touch.clientY;
                
                const rect = this.button.getBoundingClientRect();
                this.initialX = rect.left;
                this.initialY = rect.top;
                
                this.button.classList.add('dragging');
                e.preventDefault();
            });
            
            this.button.addEventListener('touchmove', (e) => {
                const touch = e.touches[0];
                const deltaX = touch.clientX - this.startX;
                const deltaY = touch.clientY - this.startY;
                
                if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                    this.isDragging = true;
                    
                    let newX = this.initialX + deltaX;
                    let newY = this.initialY + deltaY;
                    
                    const maxX = window.innerWidth - 60;
                    const maxY = window.innerHeight - 60;
                    
                    newX = Math.max(0, Math.min(maxX, newX));
                    newY = Math.max(0, Math.min(maxY, newY));
                    
                    this.button.style.left = newX + 'px';
                    this.button.style.top = newY + 'px';
                    this.button.style.right = 'auto';
                    this.button.style.bottom = 'auto';
                }
                e.preventDefault();
            });
            
            this.button.addEventListener('touchend', (e) => {
                this.button.classList.remove('dragging');
                
                if (!this.isDragging) {
                    this.toggleChat();
                }
                
                this.isDragging = false;
                e.preventDefault();
            });
            
            // 关闭按钮事件
            this.closeButton.addEventListener('click', () => this.closeChat());
            
            // 发送按钮事件
            this.sendButton.addEventListener('click', () => this.sendMessage());
            
            // 回车发送
            this.textarea.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });
            
            // 自动调整输入框高度
            this.textarea.addEventListener('input', () => this.adjustTextareaHeight());
        }

        toggleChat() {
            if (this.isOpen) {
                this.closeChat();
            } else {
                this.openChat();
            }
        }

        openChat() {
            this.isOpen = true;
            this.button.style.display = 'none';
            this.chatWindow.classList.remove('hidden');
            
            // 如果是首次打开且没有消息，添加欢迎消息
            if (this.messages.length === 0) {
                this.addMessage('assistant', this.options.welcomeMessage);
            }
            
            setTimeout(() => {
                this.textarea.focus();
            }, 300);
        }

        closeChat() {
            this.isOpen = false;
            this.button.style.display = 'flex';
            this.chatWindow.classList.add('hidden');
        }

        addMessage(role, content, isLoading = false) {
            const message = { role, content, isLoading };
            this.messages.push(message);
            
            const messageDiv = document.createElement('div');
            messageDiv.className = `cs-message ${role}`;
            
            const bubbleDiv = document.createElement('div');
            bubbleDiv.className = `cs-message-bubble ${role}`;
            
            if (isLoading) {
                bubbleDiv.innerHTML = '<span class="cs-loading">正在输入</span>';
            } else {
                bubbleDiv.textContent = content;
            }
            
            messageDiv.appendChild(bubbleDiv);
            this.messagesContainer.appendChild(messageDiv);
            
            this.scrollToBottom();
            return messageDiv;
        }

        async sendMessage() {
            const message = this.textarea.value.trim();
            if (!message || this.isLoading) return;
            
            // 清空输入框
            this.textarea.value = '';
            this.adjustTextareaHeight();
            
            // 添加用户消息
            this.addMessage('user', message);
            
            // 添加加载消息
            const loadingMessageDiv = this.addMessage('assistant', '', true);
            
            this.isLoading = true;
            this.sendButton.disabled = true;
            
            try {
                // 生成或获取用户会话ID
                let memoryId = localStorage.getItem('customer-service-memory-id');
                if (!memoryId) {
                    memoryId = 'user-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
                    localStorage.setItem('customer-service-memory-id', memoryId);
                }
                
                const response = await fetch(`${this.options.apiUrl}?message=${encodeURIComponent(message)}&memoryId=${encodeURIComponent(memoryId)}`);
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                // 移除加载消息
                loadingMessageDiv.remove();
                this.messages.pop();
                
                // 处理流式响应
                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                
                // 创建助手消息
                const assistantMessageDiv = this.addMessage('assistant', '');
                const bubbleDiv = assistantMessageDiv.querySelector('.cs-message-bubble');
                
                let fullResponse = '';
                
                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;
                    
                    const chunk = decoder.decode(value, { stream: true });
                    fullResponse += chunk;
                    bubbleDiv.textContent = fullResponse;
                    this.scrollToBottom();
                }
                
                // 更新消息数组
                this.messages[this.messages.length - 1].content = fullResponse;
                
            } catch (error) {
                console.error('Error sending message:', error);
                
                // 移除加载消息
                loadingMessageDiv.remove();
                this.messages.pop();
                
                // 添加错误消息
                this.addMessage('assistant', '抱歉，服务暂时不可用，请稍后再试。');
            } finally {
                this.isLoading = false;
                this.sendButton.disabled = false;
                this.textarea.focus();
            }
        }

        scrollToBottom() {
            setTimeout(() => {
                this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
            }, 100);
        }

        adjustTextareaHeight() {
            this.textarea.style.height = 'auto';
            this.textarea.style.height = Math.min(this.textarea.scrollHeight, 120) + 'px';
        }

        destroy() {
            if (this.container && this.container.parentNode) {
                this.container.parentNode.removeChild(this.container);
            }
        }
    }

    // 全局暴露
    window.CustomerServiceWidget = CustomerServiceWidget;

    // 自动初始化（如果页面加载完成）
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            if (!window.customerService) {
                window.customerService = new CustomerServiceWidget({
                    welcomeMessage: '您好！欢迎来到黑马点评，有什么可以帮助您的吗？'
                });
            }
        });
    } else {
        if (!window.customerService) {
            window.customerService = new CustomerServiceWidget({
                welcomeMessage: '您好！欢迎来到黑马点评，有什么可以帮助您的吗？'
            });
        }
    }

})();
