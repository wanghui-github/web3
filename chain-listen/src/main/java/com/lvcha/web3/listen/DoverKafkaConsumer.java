package com.lvcha.web3.listen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvcha.web3.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
public class DoverKafkaConsumer {

    @Resource
    private ObjectMapper mapper;
    @Resource
    private MessageHandler messageHandler;
    @KafkaListener(topics = "${spring.kafka.dover-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMessage(String message) throws JsonProcessingException {
        log.info("Received message: {}", message);
        // 处理接收到的消息
        Map transactionObject = mapper.readValue(message, Map.class);

        messageHandler.handleMessage(transactionObject);
    }
}
