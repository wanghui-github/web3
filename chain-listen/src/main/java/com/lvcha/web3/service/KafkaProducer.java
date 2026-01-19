package com.lvcha.web3.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.Resource;

@Component
public class KafkaProducer {

    @Value("${spring.kafka.dover-topic}")
    private String doverTopic;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息（异步）
     * @param message 要发送的消息内容
     */
    public void sendMessage(String message) {
        // 发送消息到指定Topic
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(doverTopic, message);

        // 异步回调处理发送结果
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                System.out.println("消息发送成功：" + message +
                    "，offset：" + result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                System.err.println("消息发送失败：" + message + "，原因：" + ex.getMessage());
            }
        });
    }

    /**
     * 发送消息（同步）
     * @param message 要发送的消息内容
     * @throws Exception 发送异常
     */
    public void sendMessageSync(String message) throws Exception {
        SendResult<String, String> result = kafkaTemplate.send(doverTopic, message).get();
        System.out.println("同步发送消息成功，partition：" + result.getRecordMetadata().partition());
    }
}
