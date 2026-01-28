package com.lvcha.web3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvcha.web3.config.Web3Config;
import com.lvcha.web3.contract.generated.Lp;
import com.lvcha.web3.contract.generated.SimpleStorage;
import com.lvcha.web3.contract.generated.TseToken;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {
    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final Web3Config ethereumConfig;
    private SimpleStorage tseContract;
    private Lp lpContract;
    private TseToken tokenContract;
    private List<String> dogList=new ArrayList<>();

    private Disposable subscription;

    @Value("${php.dog-url}")
    private String dogUrl;

    @Autowired
    KafkaProducer kafkaProducer;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    RestTemplate restTemplate;



    @PostConstruct
    public void init() {
        tseContract = SimpleStorage.load(
            ethereumConfig.getTseContractAddress(),
            web3j,
            credentials,
            gasProvider
        );
        lpContract = Lp.load(
            ethereumConfig.getLpContractAddress(),
            web3j,
            credentials,
            gasProvider
        );
        tokenContract = TseToken.load(
            ethereumConfig.getTokenContractAddress(),
            web3j,
            credentials,
            gasProvider
        );
        log.info("合约 Contract initialized with address: {}", ethereumConfig.getTseContractAddress());
        log.info("SWAP Contract initialized with address: {}", ethereumConfig.getLpContractAddress());
        log.info("TSE Token Contract initialized with address: {}", ethereumConfig.getTokenContractAddress());
        // 项目启动时开始监听
        startListening();

    }

    private void startListening() {
        log.info("Starting dcnystake listener...");
        // 在创建新订阅前取消旧订阅
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("Disposed existing subscription");
        }
        subscription = dcnystakeListen()
            .retryWhen(errors ->
                errors.delay(10, TimeUnit.SECONDS)  // 延迟5秒后重试
            )
            .subscribe(
                detail -> log.info("Detected dcnystake transaction: {}", detail),
                error -> {
                    log.error("Error in dcnystake listener, will restart...", error);
                    // 避免无限递归，使用调度器延时重启
                    Flowable.timer(5, TimeUnit.SECONDS)
                        .subscribe(tick -> startListening());
                },
                () -> log.info("Dcnystake listener completed")
            );
    }
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void scheduledRestart() {
        startListening();
    }


    // 添加资源清理方法
    public void stopListening() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("Dcnystake listener stopped");
        }
    }

    //获取赎回金额
    public BigInteger getUnstakeFromSn(String address,BigInteger  sn) throws Exception {
        return tseContract.getlpusdt(sn, address).send();
    }


    public BigInteger getyj(String account) {
        try {
            return tseContract.getmyyj(account).send();
        } catch (Exception e) {
            log.error("Error getting value from contract", e);
            throw new RuntimeException("Failed to get value from contract", e);
        }
    }

    public List<String> getPid(String account){
        try {
            return tseContract.getallpid(Collections.singletonList(account)).send();
        } catch (Exception e) {
            log.error("Error getting value from contract", e);
            throw new RuntimeException("Failed to get value from contract", e);
        }
    }

    public TransactionReceipt dcnystake(BigInteger amountdcny, BigInteger days) throws Exception {

        BigInteger amountInWei = amountdcny.multiply(BigInteger.valueOf(1_000_000_000_000_000_000L)); // 10^18

        return tseContract.dcnystake(amountInWei, days, BigInteger.valueOf(0)).send();
    }

    public void getTodayTrans() throws IOException {
        // 获取今天的区块范围
        BigInteger startBlock = getStartBlockOfDay();
        BigInteger endBlock = getEndBlockOfDay();

// 查询指定区块范围内的交易
        EthBlock.Block startBlockInfo = web3j.ethGetBlockByNumber(
            DefaultBlockParameter.valueOf(startBlock), false).send().getBlock();
        EthBlock.Block endBlockInfo = web3j.ethGetBlockByNumber(
            DefaultBlockParameter.valueOf(endBlock), false).send().getBlock();

// 遍历区块查找相关交易
        for (BigInteger i = startBlock; i.compareTo(endBlock) <= 0; i = i.add(BigInteger.ONE)) {
            EthBlock.Block block = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(i), true).send().getBlock();

//            // 检查区块中的交易是否调用了 dcnystake 方法
            for (EthBlock.TransactionResult transaction : block.getTransactions()) {
                if (transaction instanceof EthBlock.TransactionObject) {
                    EthBlock.TransactionObject tx = (EthBlock.TransactionObject) transaction;
                    log.info("Checking transaction: {}", tx);
                    // dcnystake(uint256,uint256,uint256) 的方法签名
                    String methodSignature = "5e0d443f"; // 通过keccak256("dcnystake(uint256,uint256,uint256)")计算得出

                    if (tx.getInput().startsWith("0x" + methodSignature)) {
                        log.info("Checking dcnystake: {}", tx);
                    }

                }
            }
        }

    }



    public  Flowable<Void> dcnystakeListen() {
        return web3j.blockFlowable(true)
            .onErrorResumeNext(throwable -> {
                log.error("Error in block listening, attempting to continue", throwable);
                return web3j.blockFlowable(true);
            })
            .retryWhen(errors ->
                errors.zipWith(Flowable.range(1, 3), (error, attempt) -> attempt)
                    .flatMap(attempt -> {
                        log.warn("Retry attempt {} after error", attempt);
                        return Flowable.timer(5 * attempt, TimeUnit.SECONDS);
                    })
            )
            .flatMap(block -> {

                List<EthBlock.TransactionResult> transactions = block.getBlock().getTransactions();

                for (EthBlock.TransactionResult transactionResult : transactions) {
                    if (transactionResult instanceof EthBlock.TransactionObject) {
                        EthBlock.TransactionObject tx = (EthBlock.TransactionObject) transactionResult;
                       //监控SWAP合约
                        if (tx.getTo().equalsIgnoreCase(ethereumConfig.getLpContractAddress())
                        && tx.getInput().startsWith("0x38ed1739")) {
                            log.info("Checking SWAP transaction: {}", tx.getInput());
                            addDog(tx.getFrom());
//                            Map<String, Object> map = new HashMap<>();
//                            map.put("tx", tx);
//                            map.put("block", block.getBlock());
//                            kafkaProducer.sendMessage(mapper.writeValueAsString(map));
                        }

                    }
                }

            return Flowable.empty();
            });
    }

    @Async
    public void addDog(String  address)  {
        log.info("addDog: {} start", address);
        try {
            if(ethereumConfig.getWhiteList().contains( address)){
                return;
            }

            if(!CollectionUtils.isEmpty(dogList) && dogList.contains( address)){
                return;
            }
            restTemplate.getForObject(dogUrl+"?wallet={address}", String.class,address);
            dogList.add( address);
        } catch (Exception e) {
            log.error("Error addDog: {}", address, e);
            throw new RuntimeException(e);
        }

    }




//    public Flowable<StakeTransactionDetail> dcnystakeListen() throws ConnectException {
//        // dcnystake(uint256,uint256,uint256) 的方法签名
//        String methodSignature = "5e0d443f"; // 通过keccak256("dcnystake(uint256,uint256,uint256)")计算得出
//
//        // 监听新区块中的交易
//        // 使用 WebSocket 订阅替代 HTTP 过滤器
//        WebSocketService webSocketService = new WebSocketService("ws://test-rpc.bjwmls.com:8546", false);
//        webSocketService.connect();
//        Web3j ws = Web3j.build(webSocketService);
//
//        return ws.blockFlowable(true)
//            .flatMap(block -> {
//                List<StakeTransactionDetail> details=new ArrayList<>();
//                List<EthBlock.TransactionResult> transactions = block.getBlock().getTransactions();
//
//                for (EthBlock.TransactionResult transactionResult : transactions) {
//                    if (transactionResult instanceof EthBlock.TransactionObject) {
//                        EthBlock.TransactionObject tx = (EthBlock.TransactionObject) transactionResult;
//                        if (tx.getInput().startsWith("0x" + methodSignature)) {
//                            log.info("dcnystake transaction detected: {}", tx.getHash());
//                            StakeTransactionDetail detail = parseDcnystakeTransaction(tx, block.getBlock());
//                            log.info("Parsed dcnystake transaction: {}", detail);
//                            if (detail != null) {
//                                details.add(detail);
//                            }
//                        }
//                    }
//                }
//                return Flowable.fromIterable(details);
//            });
//
//    }





    /**
     * 获取今天0点时刻的区块号
     */
    private BigInteger getStartBlockOfDay() {
        try {
            // 获取今天的开始时间戳（UTC时区）
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
            long startOfDayTimestamp = startOfDay.getEpochSecond();

            // 获取当前最新区块
            EthBlock latestBlockResponse = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf("latest"), false).send();
            BigInteger currentBlockNumber = latestBlockResponse.getBlock().getNumber();

            // 如果当前区块时间戳小于目标时间，则返回创世区块
            EthBlock.Block currentBlock = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(currentBlockNumber), false).send().getBlock();
            if (currentBlock.getTimestamp().longValue() < startOfDayTimestamp) {
                return BigInteger.ZERO;
            }

            // 估算开始区块号
            BigInteger estimatedBlock = estimateBlockByTime(startOfDayTimestamp);

            // 使用二分法精确查找
            return findBlockByTimestamp(startOfDayTimestamp, BigInteger.ZERO, currentBlockNumber);
        } catch (IOException e) {
            log.error("获取今天开始区块失败", e);
            throw new RuntimeException("获取今天开始区块失败", e);
        }
    }

    /**
     * 获取今天23:59:59时刻的区块号
     */
    private BigInteger getEndBlockOfDay() {
        try {
            // 获取今天的结束时间戳（UTC时区）
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusSeconds(1).toInstant();
            long endOfDayTimestamp = endOfDay.getEpochSecond();

            // 获取当前最新区块
            EthBlock latestBlockResponse = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf("latest"), false).send();
            BigInteger currentBlockNumber = latestBlockResponse.getBlock().getNumber();

            // 获取当前区块时间戳
            EthBlock.Block currentBlock = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(currentBlockNumber), false).send().getBlock();

            // 如果当前时间还没到今天结束时间，则使用当前区块作为结束区块
            if (currentBlock.getTimestamp().longValue() < endOfDayTimestamp) {
                return currentBlockNumber;
            }

            // 使用二分法精确查找
            return findBlockByTimestamp(endOfDayTimestamp, BigInteger.ZERO, currentBlockNumber);
        } catch (IOException e) {
            log.error("获取今天结束区块失败", e);
            throw new RuntimeException("获取今天结束区块失败", e);
        }
    }

    /**
     * 根据时间戳查找区块号
     */
    private BigInteger findBlockByTimestamp(long targetTimestamp, BigInteger startBlock, BigInteger endBlock) throws IOException {
        BigInteger left = startBlock;
        BigInteger right = endBlock;

        while (left.compareTo(right) <= 0) {
            BigInteger mid = left.add(right).shiftRight(1); // 相当于除以2

            EthBlock.Block block = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(mid), false).send().getBlock();
            long blockTimestamp = block.getTimestamp().longValue();

            if (blockTimestamp < targetTimestamp) {
                left = mid.add(BigInteger.ONE);
            } else if (blockTimestamp > targetTimestamp) {
                right = mid.subtract(BigInteger.ONE);
            } else {
                // 找到了精确匹配的时间戳
                return mid;
            }
        }

        // 返回最接近的区块号
        return right.max(BigInteger.ZERO);
    }

    /**
     * 根据时间戳估算区块号
     */
    private BigInteger estimateBlockByTime(long targetTimestamp) throws IOException {
        // 获取最新区块
        EthBlock latestBlockResponse = web3j.ethGetBlockByNumber(
            DefaultBlockParameter.valueOf("latest"), false).send();
        EthBlock.Block latestBlock = latestBlockResponse.getBlock();
        BigInteger currentBlockNumber = latestBlock.getNumber();
        long currentTimestamp = latestBlock.getTimestamp().longValue();

        // 获取创世区块时间戳
        EthBlock.Block genesisBlock = web3j.ethGetBlockByNumber(
            DefaultBlockParameter.valueOf(BigInteger.ZERO), false).send().getBlock();
        long genesisTimestamp = genesisBlock.getTimestamp().longValue();

        // 计算平均出块时间
        long timeDiff = currentTimestamp - genesisTimestamp;
        long blockDiff = currentBlockNumber.longValue();
        double avgBlockTime = timeDiff / Math.max(blockDiff, 1.0);

        // 估算目标区块号
        long timeToTarget = targetTimestamp - genesisTimestamp;
        BigInteger estimatedBlock = BigInteger.valueOf((long)(timeToTarget / avgBlockTime));

        // 确保估算的区块号在合理范围内
        if (estimatedBlock.compareTo(BigInteger.ZERO) < 0) {
            return BigInteger.ZERO;
        }
        if (estimatedBlock.compareTo(currentBlockNumber) > 0) {
            return currentBlockNumber;
        }

        return estimatedBlock;
    }



    /**
     * 部署新的SimpleStorage合约
     * @return 部署的合约地址
     */
    public String deployContract() {
        try {
            log.info("开始部署新的SimpleStorage合约...");
            SimpleStorage newContract = SimpleStorage.deploy(
                web3j,
                credentials,
                gasProvider
            ).send();

            String contractAddress = newContract.getContractAddress();
            log.info("合约部署成功! 合约地址: {}", contractAddress);
            return contractAddress;
        } catch (Exception e) {
            log.error("部署合约失败", e);
            throw new RuntimeException("部署合约失败", e);
        }
    }

}
