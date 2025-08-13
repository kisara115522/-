package com.hmdp.controller;

import com.hmdp.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @RequestMapping(value = "/chat",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId,String message) {
        Flux<String> result = aiChatService.chat(memoryId,message);
        return result;

    }
}
