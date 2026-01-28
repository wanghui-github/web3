package com.lvcha.web3.service;

import io.reactivex.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;
import com.lvcha.web3.config.Web3Config;
import com.lvcha.web3.contract.generated.SimpleStorage;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractEventListener {
    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final Web3Config ethereumConfig;

    private SimpleStorage tseContract;
    private Disposable eventSubscription;

    @PostConstruct
    public void init() {
        tseContract = SimpleStorage.load(
            ethereumConfig.getTseContractAddress(),
            web3j,
            credentials,
            gasProvider
        );

        // 订阅ValueChanged事件
//        eventSubscription = contract.valueChangedEventFlowable(
//                DefaultBlockParameter.valueOf("latest"),
//                DefaultBlockParameter.valueOf("latest"))
//            .subscribe(
//                event -> {
//                    log.info("ValueChanged事件触发:");
//                    log.info("新值: {}", event.newValue);
//                    log.info("修改者: {}", event.changedBy);
//
//                    // 在这里可以添加自定义的业务逻辑处理
//                },
//                error -> log.error("事件监听发生错误", error)
//            );
//
//        log.info("合约事件监听已启动");
    }

    @PreDestroy
    public void cleanup() {
        if (eventSubscription != null && !eventSubscription.isDisposed()) {
            eventSubscription.dispose();
            log.info("合约事件监听已关闭");
        }
    }
}
