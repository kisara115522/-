package com.hmdp.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@AiService
public interface AiChatService {

    //@SystemMessage()
    public Flux<String> chat(@MemoryId String memortyId,@UserMessage String message);
}
