package com.hmdp.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI工具配置类
 * 用于配置AI工具相关的Bean
 * 
 * 注意：LangChain4j的@AiService注解会自动发现并注入标记为@Component的工具类
 * 因此我们的工具类只需要标记@Component注解即可被自动使用
 */
@Configuration
public class AIToolConfig {
    // 工具类通过@Component注解自动注册，无需额外配置
}
