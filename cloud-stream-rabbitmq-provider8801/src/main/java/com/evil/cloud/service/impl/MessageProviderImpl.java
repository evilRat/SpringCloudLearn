package com.evil.cloud.service.impl;

import com.evil.cloud.service.IMessageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import javax.annotation.Resource;
import java.util.UUID;

@EnableBinding(Source.class)
@Slf4j
public class MessageProviderImpl implements IMessageProvider {

    //注入消息发送管道
    @Resource
    private MessageChannel output;

    @Override
    public String send() {
        String id = UUID.randomUUID().toString();
        output.send(MessageBuilder.withPayload(id).build());
        log.info("************发送消息：" + id);
        return id;
    }
}
