package com.hmdp.repository;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
public class RedisChatMemoryRepository implements ChatMemoryStore {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        //从redis里取出对应memoryid的chatMessage对象
        String json = stringRedisTemplate.opsForValue().get(memoryId);
        //调用反序列化工具将json转为java对象
        List<ChatMessage> list= ChatMessageDeserializer.messagesFromJson(json);
        return list;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        //将list序列化成json文本
        String json = ChatMessageSerializer.messagesToJson(list);
        //存入redis
        stringRedisTemplate.opsForValue().set(memoryId.toString(),json, Duration.ofDays(1));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        //直接在redis里删除这个数据
        stringRedisTemplate.delete(memoryId.toString());
    }
}
