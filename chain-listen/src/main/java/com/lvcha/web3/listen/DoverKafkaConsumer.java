package com.lvcha.web3.listen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DoverKafkaConsumer {
//
//    @Resource
//    private ObjectMapper mapper;
//    @Resource
//    private MessageHandler messageHandler;
//    @KafkaListener(topics = "${spring.kafka.dover-topic}", groupId = "${spring.kafka.consumer.group-id}")
//    public void consumeMessage(String message) throws JsonProcessingException {
//        log.info("Received message: {}", message);
//        // 处理接收到的消息
//        Map transactionObject = mapper.readValue(message, Map.class);
//
//        messageHandler.handleMessage(transactionObject);
//    }
}
