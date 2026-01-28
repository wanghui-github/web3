package com.lvcha.web3.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
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
public class Web3Config {
    @Value("${ethereum.network.url}")
    private String networkUrl;

    @Value("${ethereum.account.privateKey}")
    private String privateKey;

    private BigInteger gasPrice;

    private BigInteger gasLimit;

    @Value("${ethereum.contract.lp-address}")
    private String lpContractAddress;

    @Value("${ethereum.contract.tse-address}")
    private String tseContractAddress;

    @Value("${ethereum.contract.token-address}")
    private String tokenContractAddress;

    private List<String> whiteList;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(networkUrl));
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
