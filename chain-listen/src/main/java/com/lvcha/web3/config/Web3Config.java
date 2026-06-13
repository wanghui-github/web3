package com.lvcha.web3.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ethereum")
@Data
@Slf4j
public class Web3Config {
    @Value("${ethereum.network.url}")
    private String networkUrl;
    @Value("${ethereum.network.bsc-ws-url}")
    private String wsUrl;
    @Value("${ethereum.network.bsc-http-url}")
    private String httpUrl;

    @Value("${ethereum.account.privateKey}")
    private String privateKey;

    private BigInteger gasPrice;

    private BigInteger gasLimit;

    @Value("${ethereum.contract.lp-address}")
    private String lpContractAddress;

    @Value("${ethereum.contract.tse-address-old}")
    private String tseContractAddressOld;
    @Value("${ethereum.contract.tse-address-new}")
    private String tseContractAddressNew;

    @Value("${ethereum.contract.token-address}")
    private String tokenContractAddress;

    @Value("${ethereum.contract.lax-queue}")
    private String laxQueueContractAddress;
    @Value("${ethereum.contract.lax-queue-new}")
    private String laxQueueNew;

    @Value("${ethereum.dcny.contract}")
    private String dcnyContractAddress;
    @Value("${ethereum.dcny.from.address}")
    private String transferFromAddress;
    @Value("${ethereum.dcny.from.privateKey}")
    private String transferFromKey;

    private List<String> whiteList;
    private List<String> toAddress;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(networkUrl));
    }

//    @Bean
//    public Web3j bscWsWeb3j() throws ConnectException, InterruptedException {
//
//            log.info("正在建立 BSC WebSocket 连接：{}", wsUrl);
//
//        org.web3j.protocol.websocket.WebSocketService wsService =
//                new org.web3j.protocol.websocket.WebSocketService(wsUrl, true);
//        CountDownLatch connectionLatch = new CountDownLatch(1);
//        AtomicBoolean isConnected = new AtomicBoolean(false);
//        AtomicReference<Throwable> lastError = new AtomicReference<>();
//
//        wsService.connect(
//            message -> {
//                log.debug("收到 WebSocket 消息：{}",
//                    message.length() > 50 ? message.substring(0, 50) + "..." : message);
//            },
//            throwable -> {
//                log.error("❌ WebSocket 发生错误: {}", throwable.getMessage(), throwable);
//                lastError.set(throwable);
//                if (!isConnected.get()) {
//                    connectionLatch.countDown();
//                }
//            },
//            () -> {
//                log.info("✅ WebSocket 连接成功!");
//                isConnected.set(true);
//                connectionLatch.countDown();
//            }
//        );
//        boolean connected = connectionLatch.await(60, TimeUnit.SECONDS);
//        if (!connected) {
//            log.error("⏰ WebSocket 连接超时 (60 秒)");
//        }
//
//        log.info("🎉 BSC WebSocket 服务初始化完成！");
//
//        return Web3j.build(wsService);
//
//
//    }
    @Bean
    public Web3j bscHttpWeb3j() {
        // BSC 节点列表，按优先级排序
        String[] httpNodes = {
            httpUrl != null && !httpUrl.isEmpty() ? httpUrl :
            "https://bsc.publicnode.com"
        };
        for (String nodeUrl : httpNodes) {
            try {
                log.info("尝试连接 BSC HTTP 节点：{}", nodeUrl);
                HttpService httpService = new HttpService(nodeUrl);
                Web3j testWeb3j = Web3j.build(httpService);

                // 测试连接是否可用
                BigInteger blockNumber = testWeb3j.ethBlockNumber().send().getBlockNumber();
                log.info("成功连接到 BSC 节点：{}, 当前区块：{}", nodeUrl, blockNumber);

                // 关闭测试连接，返回真正的 Web3j 实例
                testWeb3j.shutdown();
                return Web3j.build(new HttpService(nodeUrl));

            } catch (Exception e) {
                log.warn("BSC 节点 {} 连接失败：{}", nodeUrl, e.getMessage());
            }
        }

        throw new RuntimeException("所有 BSC HTTP 节点都无法连接");
    }


    @Bean
    public Credentials credentials() {
        return Credentials.create(privateKey);
    }

    @Bean
    public ContractGasProvider contractGasProvider() {
        return new StaticGasProvider(gasPrice, gasLimit);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 配置日期格式
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 空值处理
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
