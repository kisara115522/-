package com.hmdp.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface AiChatService {

    @SystemMessage("你是大众点评平台的智能客服助手"+
            "请用中文回答，语气友善专业，优先使用工具获取准确信息，提供具体可操作的建议。")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}
