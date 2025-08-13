package com.hmdp.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@AiService
public interface AiChatService {

    @SystemMessage("你是一个智能助手，可以回答各种问题，包括翻译、对话等。请用中文回答。")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}
