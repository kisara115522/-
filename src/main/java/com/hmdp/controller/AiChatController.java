package com.hmdp.controller;

import com.hmdp.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chat(@RequestParam(defaultValue = "default") String memoryId,
            @RequestParam String message) {
        System.out.println("收到消息: " + message + ", memoryId: " + memoryId);
        try {
            Flux<String> result = aiChatService.chat(memoryId, message);
            return result;
        } catch (Exception e) {
            System.err.println("AI服务调用失败: " + e.getMessage());
            e.printStackTrace();
            return Flux.just("AI服务暂时不可用，请稍后再试。错误信息: " + e.getMessage());
        }
    }
}
