package com.lvcha.web3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvcha.web3.config.Web3Config;
import com.lvcha.web3.contract.generated.Laxqueue;
import com.lvcha.web3.contract.generated.Laxqueuenew;
import com.lvcha.web3.contract.generated.SimpleStorage;
import com.lvcha.web3.mapper.Web3DictMapper;
import com.lvcha.web3.pojo.LaxQueueDetail;
import com.lvcha.web3.pojo.Web3Dict;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.tx.gas.ContractGasProvider;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {
    private final Web3j web3j;
//    @Qualifier("bscWsWeb3j")
//    private final Web3j bscWsWeb3j;
    @Qualifier("bscHttpWeb3j")
    private final Web3j bscHttpWeb3j;

    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final Web3Config ethereumConfig;
//    private SimpleStorage tseContractOld;
//    private SimpleStorage tseContractNew;
    private List<String> dogList=new ArrayList<>();
    private Laxqueue laxqueue;
    private Laxqueuenew laxqueueNew;
    private AtomicBoolean isPolling = new AtomicBoolean(false);
    private AtomicLong lastProcessedBlock = new AtomicLong(0);
    private AtomicLong lastNewBlock = new AtomicLong(0);
    private final int BLOCK_STEP = 5000; // 每次查询的区块步长
    private final String DICT_TYPE = "LAX_QUEUED_EVENT";
    private final String DICT_TYPE_NEW = "LAX_QUEUED_EVENT_NEW";
    private final String DICT_NAME = "LAST_BLOCK";
    private int processedBatchCount = 0; // 已处理的批次数
    private int processedNewBatchCount=0;
    private final int SAVE_INTERVAL = 10; // 每 10 批保存一次数据库
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Value("${php.dog-url}")
    private String dogUrl;
    @Autowired
    KafkaProducer kafkaProducer;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    RestTemplate restTemplate;
    @Resource
    private LaxQueueService laxQueueService;
    @Resource
    private Web3DictMapper web3DictMapper;


    @PostConstruct
    public void init() throws Exception {
//        dcnyContract=Dcny.load(
//            ethereumConfig.getDcnyContractAddress(),
//            web3j,
//            credentials,
//            gasProvider
//        );
//        tseContractOld = SimpleStorage.load(
//            ethereumConfig.getTseContractAddressOld(),
//            web3j,
//            credentials,
//            gasProvider
//        );
//        tseContractNew = SimpleStorage.load(
//            ethereumConfig.getTseContractAddressNew(),
//            web3j,
//            credentials,
//            gasProvider
//        );
//        lpContract = Lp.load(
//            ethereumConfig.getLpContractAddress(),
//            web3j,
//            credentials,
//            gasProvider
//        );
//        tokenContract = TseToken.load(
//            ethereumConfig.getTokenContractAddress(),
//            web3j,
//            credentials,
//            gasProvider
//        );


        laxqueue=Laxqueue.load(
            ethereumConfig.getLaxQueueContractAddress(),
            bscHttpWeb3j,
            credentials,
            gasProvider
        );
        laxqueueNew=Laxqueuenew.load(
            ethereumConfig.getLaxQueueNew(),
            bscHttpWeb3j,
            credentials,
            gasProvider
        );

        startLaxQueuePolling();
    }

    private void startLaxQueuePolling() {
        if (isPolling.getAndSet(true)) {
            log.info("轮询任务已在运行");
            return;
        }
        Web3Dict lax = web3DictMapper.getDictByTypeAndName(DICT_TYPE, DICT_NAME);
        Web3Dict laxNew = web3DictMapper.getDictByTypeAndName(DICT_TYPE_NEW, DICT_NAME);
        if (lax != null && lax.getDictValue() != null) {
            long savedBlock = Long.parseLong(lax.getDictValue());
            lastProcessedBlock.set(savedBlock);
            log.info("✅ 从数据库加载区块进度： 最后区块={}",savedBlock);
        }else{
            long defaultBlock = 88760198L;
            lastProcessedBlock.set(defaultBlock);
            log.info("⚠️ 未找到历史记录，使用默认起始区块：{}", defaultBlock);
        }
        if (laxNew != null && laxNew.getDictValue() != null) {
            long savedBlock = Long.parseLong(laxNew.getDictValue());
            lastNewBlock.set(savedBlock);
            log.info("✅ 从数据库加载区块进度： 最后区块={}",savedBlock);
        }else{
            long defaultBlock = 91155133L;
            lastNewBlock.set(defaultBlock);
            log.info("⚠️ 未找到历史记录，使用默认起始区块：{}", defaultBlock);
        }

        pollEvents(lastProcessedBlock.get(),false);
        pollEvents(lastNewBlock.get(),true);
    }


    public void pollEvents(long fromBlock,boolean newLax) {
        // 获取当前最新区块号
        bscHttpWeb3j.ethBlockNumber().sendAsync()
            .thenAccept(blockResponse  -> {
                long latest = blockResponse .getBlockNumber().longValue();
                long start = fromBlock + 1;
                if (start > latest) {
                    // 已追到最新，休眠后继续
                    try {
                        Thread.sleep(5 * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    pollEvents(latest,newLax);
                    return;
                }
                long toBlock = Math.min(start + BLOCK_STEP - 1, latest);

                CompletableFuture<?> eventsFuture = newLax ?
                    queryEventsNew(start, toBlock) :
                    queryEvents(start, toBlock);

                eventsFuture.thenAccept(events -> {
                    // 处理事件
                    if(newLax){

                        for (Laxqueuenew.QueuedEventResponse event : (List<Laxqueuenew.QueuedEventResponse>) events) {
                            processEventNew(event);
                        }
                        long newProcessedBlock = toBlock;
                        if (events!=null) {
                            long maxBlock = ((List<Laxqueuenew.QueuedEventResponse>) events).stream()
                                .mapToLong(e -> e.log.getBlockNumber().longValue())
                                .max()
                                .orElse(toBlock);
                            newProcessedBlock = maxBlock;
                        }
                        lastNewBlock.set(newProcessedBlock);
                        processedNewBatchCount++;

                        if (processedNewBatchCount % SAVE_INTERVAL == 0) {
                            saveProgressNew(newProcessedBlock);
                            log.info("📊 [New] 已处理 {} 批，已保存到数据库，最后区块：{}",
                                processedNewBatchCount, newProcessedBlock);
                        }

                        pollEvents(newProcessedBlock, newLax);
                    }else{
                        for (Laxqueue.QueuedEventResponse event : (List<Laxqueue.QueuedEventResponse>) events) {
                            processEvent(event);
                        }

                        long newProcessedBlock = toBlock;
                        if (events!=null) {
                            long maxBlock = ((List<Laxqueue.QueuedEventResponse>) events).stream()
                                .mapToLong(e -> e.log.getBlockNumber().longValue())
                                .max()
                                .orElse(toBlock);
                            newProcessedBlock = maxBlock;
                        }
                        lastProcessedBlock.set(newProcessedBlock);
                        processedBatchCount++;

                        if (processedBatchCount % SAVE_INTERVAL == 0) {
                            saveProgress(newProcessedBlock);
                            log.info("📊 [Old] 已处理 {} 批，已保存到数据库，最后区块：{}",
                                processedBatchCount, newProcessedBlock);
                        }

                        pollEvents(newProcessedBlock, newLax);
                    }


                }).exceptionally(ex -> {
                    log.error("查询事件失败，{}秒后重试", 10, ex);
                    try {
                        Thread.sleep(10 * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    saveProgress(lastProcessedBlock.get());
                    pollEvents(lastProcessedBlock.get(),newLax);
                    return null;
                });

        }).exceptionally(ex -> {
            log.error("获取最新区块号失败", ex);
            // 延迟后重试
            Flowable.timer(10, TimeUnit.SECONDS)
                .subscribe(tick -> pollEvents(lastProcessedBlock.get(),newLax));
            return null;
        });
    }

    private void saveProgress(long blockNumber) {
        log.info("保存区块开始：{}", blockNumber);
        int ct=web3DictMapper.updateDictValueByTypeAndName(DICT_TYPE,DICT_NAME,String.valueOf(blockNumber));
        if(ct==1){
            log.info("保存区块成功：{}", blockNumber);
        }
    }
    private void saveProgressNew(long blockNumber) {
        log.info("保存新区块开始：{}", blockNumber);
        int ct=web3DictMapper.updateDictValueByTypeAndName(DICT_TYPE_NEW,DICT_NAME,String.valueOf(blockNumber));
        if(ct==1){
            log.info("保存新区块成功：{}", blockNumber);
        }
    }
    private CompletableFuture<List<Laxqueuenew.QueuedEventResponse>> queryEventsNew(long fromBlock, long toBlock) {
        EthFilter filter = new EthFilter(
            DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
            DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
            laxqueueNew.getContractAddress()
        );
        filter.addSingleTopic(EventEncoder.encode(laxqueueNew.QUEUED_EVENT));

        return bscHttpWeb3j.ethGetLogs(filter)
            .sendAsync()
            .thenApply(logs -> {
                if (logs == null || logs.getLogs() == null) {
                    return Collections.emptyList();
                }
                return logs.getLogs().stream()
                    .filter(log -> log instanceof EthLog.LogObject)
                    .filter(log -> ((EthLog.LogObject) log).getAddress().equalsIgnoreCase(laxqueueNew.getContractAddress()))
                    .map(log -> extractQueuedEventNew((EthLog.LogObject) log))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            });
    }
    private CompletableFuture<List<Laxqueue.QueuedEventResponse>> queryEvents(long fromBlock, long toBlock) {
        // 使用合约的 event 方法，通过 EthFilter 查询日志
        EthFilter filter = new EthFilter(
            DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
            DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
            laxqueue.getContractAddress()
        );
        filter.addSingleTopic(EventEncoder.encode(laxqueue.QUEUED_EVENT)); // 假设事件签名

        return bscHttpWeb3j.ethGetLogs(filter)
            .sendAsync()
            .thenApply(logs -> {
                if (logs == null || logs.getLogs() == null) {
                    return Collections.emptyList();
                }
                // 将 EthLog.LogResult 转换为 LaxQueue.QueuedEventResponse
                return logs.getLogs().stream()
                    .filter(log -> log instanceof EthLog.LogObject)
                    .filter(log -> ((EthLog.LogObject) log).getAddress().equalsIgnoreCase(laxqueue.getContractAddress()))
                    .map(log -> extractQueuedEvent((EthLog.LogObject) log))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            })
           ;

    }

    private static Laxqueuenew.QueuedEventResponse extractQueuedEventNew(EthLog.LogObject logObject) {
        try {
            if (logObject == null) {
                return null;
            }
            Log log = logObject.get();
            if (log == null) {
                return null;
            }
            List<String> indexedParams = log.getTopics();
            String data = log.getData();

            var nonIndexedValues = FunctionReturnDecoder.decode(
                data,
                Laxqueuenew.QUEUED_EVENT.getNonIndexedParameters()
            );

            Laxqueuenew.QueuedEventResponse response = new Laxqueuenew.QueuedEventResponse();

            Address user = (Address) FunctionReturnDecoder.decodeIndexedValue(
                indexedParams.get(1), new TypeReference<Address>() {});
            response.user = user.getValue();

            if (nonIndexedValues.size() >= 4) {
                response.queueIndex = (BigInteger) nonIndexedValues.get(0).getValue();
                response.amount = (BigInteger) nonIndexedValues.get(1).getValue();
                response.timestamp = (BigInteger) nonIndexedValues.get(2).getValue();
                response.stakeIndex = (BigInteger) nonIndexedValues.get(3).getValue();
            }

            response.log = log;

            return response;

        } catch (Exception e) {
            log.error("解析 Laxqueuenew Queued 事件失败：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从日志中提取 Queued 事件参数
     */
    private static Laxqueue.QueuedEventResponse extractQueuedEvent(EthLog.LogObject logObject) {
        try {
            // 检查 logObject 是否为 null
            if (logObject == null) {
                return null;
            }
            Log log = logObject.get();
            // 检查 log 是否为 null
            if (log == null) {
                return null;
            }
            // 获取 indexed 参数（从 topics 中）
            List<String> indexedParams = log.getTopics();

            // 获取 non-indexed 参数（从 data 中）
            String data = log.getData();

            // 解码 non-indexed 参数
            var nonIndexedValues = FunctionReturnDecoder.decode(
                data,
                Laxqueue.QUEUED_EVENT.getNonIndexedParameters()
            );



            // 创建事件响应对象并设置参数
            Laxqueue.QueuedEventResponse response = new Laxqueue.QueuedEventResponse();

            Address user = (Address) FunctionReturnDecoder.decodeIndexedValue(
                indexedParams.get(1), new TypeReference<Address>() {});
            response.user = user.getValue();

            // 设置 non-indexed 参数
            if (nonIndexedValues.size() >= 4) {
                response.queueIndex = (BigInteger) nonIndexedValues.get(0).getValue();
                response.amount = (BigInteger) nonIndexedValues.get(1).getValue();
                response.timestamp = (BigInteger) nonIndexedValues.get(2).getValue();
                response.stakeIndex = (BigInteger) nonIndexedValues.get(3).getValue();
            }

            // 设置日志信息
            response.log = log;

            return response;

        } catch (Exception e) {
            log.error("解析 Queued 事件失败：{}", e.getMessage(), e);
            return null;
        }
    }

    private void processEventNew(Laxqueuenew.QueuedEventResponse event) {
        try {
            LaxQueueDetail detail = new LaxQueueDetail();
            detail.setAddress(event.user);
            detail.setQueueIndex(event.queueIndex.intValue());
            detail.setAmount(event.amount.divide(BigInteger.valueOf(1000000000000000000L)));
            detail.setQueueTime(event.timestamp.longValue());
            detail.setType(event.stakeIndex.intValue());

            detail.setCreateDate(LocalDate.ofEpochDay(event.timestamp.longValue() / 86400).format(DATE_FORMATTER));
            laxQueueService.save(detail);
            log.info("事件保存成功: {}", event);
        } catch (Exception e) {
            log.error("事件保存失败", e);
        }
    }
    private void processEvent(Laxqueue.QueuedEventResponse event) {
        try {
            LaxQueueDetail detail = new LaxQueueDetail();
            detail.setAddress(event.user);
            detail.setQueueIndex(event.queueIndex.intValue());
            detail.setAmount(event.amount.divide(BigInteger.valueOf(1000000000000000000L)));
            detail.setQueueTime(event.timestamp.longValue());
            detail.setType(event.stakeIndex.intValue());

            detail.setCreateDate(LocalDate.ofEpochDay(event.timestamp.longValue() / 86400).format(DATE_FORMATTER));
            laxQueueService.save(detail);
            log.info("事件保存成功: {}", event);
        } catch (Exception e) {
            log.error("事件保存失败", e);
        }
    }
//    private void startLaxQueueListeningWithRetry() {
//        if (isListening.getAndSet(true)) {
//            log.info("监听已在运行，跳过");
//            return;
//        }
//        // 添加初始延迟，给节点稳定时间（可选）
//        Flowable.timer(3, TimeUnit.SECONDS)
//            .subscribe(tick -> {
//                try {
//                    startLaxQueueListening();
//                } catch (Exception e) {
//                    isListening.set(false);
//                    log.error("启动 LaxQueue 监听失败，10 秒后重试", e);
//                    Flowable.timer(10, TimeUnit.SECONDS)
//                        .subscribe(t -> startLaxQueueListeningWithRetry());
//                }
//            });
//    }
//
//    private void startLaxQueueListening() throws Exception {
//
//        if (laxSubscription != null && !laxSubscription.isDisposed()) {
//            try {
//                laxSubscription.dispose();
//                log.info("Disposed existing LaxQueue subscription");
//            } catch (Exception e) {
//                log.warn("处置旧订阅时出错（可能是过滤器已失效）: {}", e.getMessage());
//            }
//            laxSubscription = null; // 清空引用
//        }
//        log.info("开始监听 Queued 事件，从最新区块开始...");
//        BigInteger startBlock = BigInteger.valueOf(lastProcessedBlock.get()).add(BigInteger.ONE);
//        laxSubscription=laxqueue.queuedEventFlowable(
//                DefaultBlockParameter.valueOf(startBlock),
//                DefaultBlockParameter.valueOf("latest"))
//            .subscribeOn(Schedulers.io())
//            .retryWhen(errors ->
//                errors.flatMap(error -> {
//                    log.error("LaxQueue 事件监听错误：{}", error.getMessage(), error);
//                    // 标记当前监听已停止，稍后重新启动
//                    isListening.set(false);
//                    // 延迟后尝试完全重建订阅
//                    Flowable.timer(5, TimeUnit.SECONDS)
//                        .doOnNext(tick -> {
//                            // 如果当前还有订阅，先 dispose
//                            if (laxSubscription != null && !laxSubscription.isDisposed()) {
//                                laxSubscription.dispose();
//                                laxSubscription = null;
//                            }
//                        })
//                        .subscribe(tick -> startLaxQueueListeningWithRetry(),
//                            ex -> log.error("重建订阅的定时任务失败", ex));
//                    return Flowable.empty(); // 终止当前流
//                }))
//            .subscribe(
//                event -> {
//                    log.info("queque事件触发:{}",event);
//                    try {
//                        LaxQueueDetail detail=new LaxQueueDetail();
//                        detail.setAddress(event.user);
//                        detail.setQueueIndex(event.queueIndex.intValue());
//                        detail.setAmount(event.amount.divide(BigInteger.valueOf(1000000000000000000L)));
//                        detail.setQueueTime(event.timestamp.longValue());
//                        detail.setType(event.stakeIndex.intValue());
//                        laxQueueService.save( detail);
//                        log.info("LaxQueue 事件保存成功");
//                        // 更新最后处理区块
//                        long blockNum = event.log.getBlockNumber().longValue();
//                        lastProcessedBlock.updateAndGet(cur -> Math.max(cur, blockNum));
//                    }catch (Exception e) {
//                        log.error("LaxQueue事件保存失败", e);
//                    }
//
//                },
//                error -> {
//                    // 不应到达此处（因为 retryWhen 已处理），但保留日志
//                    log.error("LaxQueue 事件流异常终止", error);
//                    isListening.set(false);
//                },
//                () -> {
//                    log.info("LaxQueue 事件流完成");
//                    isListening.set(false);
//                    Flowable.timer(5, TimeUnit.SECONDS)
//                        .subscribe(tick -> startLaxQueueListeningWithRetry());
//                }
//            );
//        // 确保标志为 true（订阅已创建）
//        isListening.set(true);
//    }


//    private void startListening() {
//        log.info("Starting dcnystake listener...");
//        // 在创建新订阅前取消旧订阅
//        if (subscription != null && !subscription.isDisposed()) {
//            subscription.dispose();
//            log.info("Disposed existing subscription");
//        }
//        subscription = dcnystakeListen()
//            .retryWhen(errors ->
//                errors.delay(10, TimeUnit.SECONDS)  // 延迟5秒后重试
//            )
//            .subscribe(
//                detail -> log.info("Detected dcnystake transaction: {}", detail),
//                error -> {
//                    log.error("Error in dcnystake listener, will restart...", error);
//                    // 避免无限递归，使用调度器延时重启
//                    Flowable.timer(5, TimeUnit.SECONDS)
//                        .subscribe(tick -> startListening());
//                },
//                () -> log.info("Dcnystake listener completed")
//            );
//    }


//    @PreDestroy
//    public void destroy() {
//        if (laxSubscription != null && !laxSubscription.isDisposed()) {
//            try {
//                laxSubscription.dispose();
//                log.info("LaxQueue subscription disposed");
//            } catch (Exception e) {
//                log.warn("处置 LaxQueue 订阅时出错（可能是过滤器已失效）: {}", e.getMessage());
//            }
//        }
//        if (subscription != null && !subscription.isDisposed()) {
//            try {
//                subscription.dispose();
//                log.info("dcnystake subscription disposed");
//            } catch (Exception e) {
//                log.warn("处置 dcnystake 订阅时出错：{}", e.getMessage());
//            }
//        }
//    }
//
//    // 添加资源清理方法
//    public void stopListening() {
//        if (subscription != null && !subscription.isDisposed()) {
//            subscription.dispose();
//            log.info("Dcnystake listener stopped");
//        }
//    }


//    public void transferDcny(){
//        List<String> to = ethereumConfig.getToAddress();
//        BigInteger transferAmount = BigInteger.valueOf(100000000000000000L); // 0.1 DCNY
//
//        for (String toAddress : to) {
//            try {
////                BigInteger currentAllowance = dcnyContract.allowance(ethereumConfig.getTransferFromAddress(), toAddress).send();
////                log.info("Current allowance : {}", currentAllowance);
////                if (currentAllowance.compareTo(transferAmount) < 0) {
////                    // 使用 increaseAllowance 而非直接 approve，避免授权覆盖
////                    TransactionReceipt approveReceipt = dcnyContract.increaseAllowance(toAddress, transferAmount.multiply(BigInteger.TEN))
////                        .send();
////                    log.info("Approve transaction receipt: {}", approveReceipt.isStatusOK());
////                }
//                // 2. 再调用 transferFrom 方法转账
//                TransactionReceipt res =dcnyContract.transfer(toAddress, transferAmount).send();
//                if(res.isStatusOK()){
//                    log.info("Transfer transaction to {} success", toAddress);
//                }
//
//
//            } catch (Exception e) {
//                log.error("Error transferring DCNY", e);
//            }
//        }
//
//    }

    //获取赎回金额
//    public BigInteger getUnstakeFromSn(String address,BigInteger  sn) throws Exception {
//        return tseContractNew.getlpusdt(sn, address).send();
//    }

//    public Tuple10 getUlp(String address,BigInteger  sn) throws Exception {
//        Tuple10<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, Boolean, BigInteger, String, BigInteger, BigInteger> lp = tseContractNew.ulp(address, sn).send();
//       return lp;
//    }

//    public Map<String,Object> getPidNeedAdd(String account) throws Exception {
//        String pid= tseContractNew.pidarr(account).send();
//        Map<String,Object> map=new HashMap<>();
//        if("0x0000000000000000000000000000000000000000".equals( pid)){
//            map.put("pid",pid);
//            return map;
//        }
//        BigInteger newYj=tseContractNew.getyj( pid).send();
//
//        BigInteger oldYj=tseContractOld.getyj( pid).send();
//
//        if(newYj.compareTo(BigInteger.ZERO)>0){
//            newYj=newYj.divide(BigInteger.valueOf(1000000000000000000L));
//
//        }
//        if(oldYj.compareTo(BigInteger.ZERO)>0){
//            oldYj=oldYj.divide(BigInteger.valueOf(1000000000000000000L));
//
//        }
//        BigInteger gap=newYj.subtract(oldYj);
//
//        if(gap.compareTo(BigInteger.valueOf(1000))<0){
//          log.info("pid is {} ,newYj is {} ,oldYj is {} ,gap is {}",pid,newYj,oldYj,gap);
//            map.put("pid",pid);
//            map.put("yj",gap);
//            return map;
//        }else{
//            return getPidNeedAdd(pid);
//        }
//
//    }
//
//
//    public BigInteger getyj(String account) {
//        try {
//            return tseContractNew.getmyyj(account).send();
//        } catch (Exception e) {
//            log.error("Error getting value from contract", e);
//            throw new RuntimeException("Failed to get value from contract", e);
//        }
//    }
//
//    public List<String> getPid(String account){
//        try {
//            return tseContractNew.getallpid(Collections.singletonList(account)).send();
//        } catch (Exception e) {
//            log.error("Error getting value from contract", e);
//            throw new RuntimeException("Failed to get value from contract", e);
//        }
//    }
//
//    public TransactionReceipt dcnystake(BigInteger amountdcny, BigInteger days) throws Exception {
//
//        BigInteger amountInWei = amountdcny.multiply(BigInteger.valueOf(1_000_000_000_000_000_000L)); // 10^18
//
//        return tseContractNew.dcnystake(amountInWei, days, BigInteger.valueOf(0)).send();
//    }
//
//    public void getTodayTrans() throws IOException {
//        // 获取今天的区块范围
//        BigInteger startBlock = getStartBlockOfDay();
//        BigInteger endBlock = getEndBlockOfDay();
//
//// 查询指定区块范围内的交易
//        EthBlock.Block startBlockInfo = web3j.ethGetBlockByNumber(
//            DefaultBlockParameter.valueOf(startBlock), false).send().getBlock();
//        EthBlock.Block endBlockInfo = web3j.ethGetBlockByNumber(
//            DefaultBlockParameter.valueOf(endBlock), false).send().getBlock();
//
//// 遍历区块查找相关交易
//        for (BigInteger i = startBlock; i.compareTo(endBlock) <= 0; i = i.add(BigInteger.ONE)) {
//            EthBlock.Block block = web3j.ethGetBlockByNumber(
//                DefaultBlockParameter.valueOf(i), true).send().getBlock();
//
////            // 检查区块中的交易是否调用了 dcnystake 方法
//            for (EthBlock.TransactionResult transaction : block.getTransactions()) {
//                if (transaction instanceof EthBlock.TransactionObject) {
//                    EthBlock.TransactionObject tx = (EthBlock.TransactionObject) transaction;
//                    log.info("Checking transaction: {}", tx);
//                    // dcnystake(uint256,uint256,uint256) 的方法签名
//                    String methodSignature = "5e0d443f"; // 通过keccak256("dcnystake(uint256,uint256,uint256)")计算得出
//
//                    if (tx.getInput().startsWith("0x" + methodSignature)) {
//                        log.info("Checking dcnystake: {}", tx);
//                    }
//
//                }
//            }
//        }
//
//    }
//
//
//
//    public  Flowable<Void> dcnystakeListen() {
//        return web3j.blockFlowable(true)
//            .onErrorResumeNext(throwable -> {
//                log.error("Error in block listening, attempting to continue", throwable);
//                return web3j.blockFlowable(true);
//            })
//            .retryWhen(errors ->
//                errors.zipWith(Flowable.range(1, 3), (error, attempt) -> attempt)
//                    .flatMap(attempt -> {
//                        log.warn("Retry attempt {} after error", attempt);
//                        return Flowable.timer(5 * attempt, TimeUnit.SECONDS);
//                    })
//            )
//            .flatMap(block -> {
//
//                List<EthBlock.TransactionResult> transactions = block.getBlock().getTransactions();
//
//                for (EthBlock.TransactionResult transactionResult : transactions) {
//                    if (transactionResult instanceof EthBlock.TransactionObject) {
//                        EthBlock.TransactionObject tx = (EthBlock.TransactionObject) transactionResult;
//                       //监控SWAP合约
//                        if (tx.getTo().equalsIgnoreCase(ethereumConfig.getLpContractAddress())
//                        && tx.getInput().startsWith("0x38ed1739")) {
//                            log.info("Checking SWAP transaction: {}", tx.getInput());
//                            addDog(tx.getFrom());
////                            Map<String, Object> map = new HashMap<>();
////                            map.put("tx", tx);
////                            map.put("block", block.getBlock());
////                            kafkaProducer.sendMessage(mapper.writeValueAsString(map));
//                        }
//
//                    }
//                }
//
//            return Flowable.empty();
//            });
//    }
//
//    @Async
//    public void addDog(String  address)  {
//        log.info("addDog: {} start", address);
//        try {
//            if(ethereumConfig.getWhiteList().contains( address)){
//                return;
//            }
//
//            if(!CollectionUtils.isEmpty(dogList) && dogList.contains( address)){
//                return;
//            }
//            restTemplate.getForObject(dogUrl+"?wallet={address}", String.class,address);
//            dogList.add( address);
//        } catch (Exception e) {
//            log.error("Error addDog: {}", address, e);
//            throw new RuntimeException(e);
//        }
//
//    }




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
//    private BigInteger getStartBlockOfDay() {
//        try {
//            // 获取今天的开始时间戳（UTC时区）
//            LocalDate today = LocalDate.now(ZoneOffset.UTC);
//            Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
//            long startOfDayTimestamp = startOfDay.getEpochSecond();
//
//            // 获取当前最新区块
//            EthBlock latestBlockResponse = web3j.ethGetBlockByNumber(
//                DefaultBlockParameter.valueOf("latest"), false).send();
//            BigInteger currentBlockNumber = latestBlockResponse.getBlock().getNumber();
//
//            // 如果当前区块时间戳小于目标时间，则返回创世区块
//            EthBlock.Block currentBlock = web3j.ethGetBlockByNumber(
//                DefaultBlockParameter.valueOf(currentBlockNumber), false).send().getBlock();
//            if (currentBlock.getTimestamp().longValue() < startOfDayTimestamp) {
//                return BigInteger.ZERO;
//            }
//
//            // 估算开始区块号
//            BigInteger estimatedBlock = estimateBlockByTime(startOfDayTimestamp);
//
//            // 使用二分法精确查找
//            return findBlockByTimestamp(startOfDayTimestamp, BigInteger.ZERO, currentBlockNumber);
//        } catch (IOException e) {
//            log.error("获取今天开始区块失败", e);
//            throw new RuntimeException("获取今天开始区块失败", e);
//        }
//    }
//
//    /**
//     * 获取今天23:59:59时刻的区块号
//     */
//    private BigInteger getEndBlockOfDay() {
//        try {
//            // 获取今天的结束时间戳（UTC时区）
//            LocalDate today = LocalDate.now(ZoneOffset.UTC);
//            Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusSeconds(1).toInstant();
//            long endOfDayTimestamp = endOfDay.getEpochSecond();
//
//            // 获取当前最新区块
//            EthBlock latestBlockResponse = web3j.ethGetBlockByNumber(
//                DefaultBlockParameter.valueOf("latest"), false).send();
//            BigInteger currentBlockNumber = latestBlockResponse.getBlock().getNumber();
//
//            // 获取当前区块时间戳
//            EthBlock.Block currentBlock = web3j.ethGetBlockByNumber(
//                DefaultBlockParameter.valueOf(currentBlockNumber), false).send().getBlock();
//
//            // 如果当前时间还没到今天结束时间，则使用当前区块作为结束区块
//            if (currentBlock.getTimestamp().longValue() < endOfDayTimestamp) {
//                return currentBlockNumber;
//            }
//
//            // 使用二分法精确查找
//            return findBlockByTimestamp(endOfDayTimestamp, BigInteger.ZERO, currentBlockNumber);
//        } catch (IOException e) {
//            log.error("获取今天结束区块失败", e);
//            throw new RuntimeException("获取今天结束区块失败", e);
//        }
//    }
//
//    /**
//     * 根据时间戳查找区块号
//     */
//    private BigInteger findBlockByTimestamp(long targetTimestamp, BigInteger startBlock, BigInteger endBlock) throws IOException {
//        BigInteger left = startBlock;
//        BigInteger right = endBlock;
//
//        while (left.compareTo(right) <= 0) {
//            BigInteger mid = left.add(right).shiftRight(1); // 相当于除以2
//
//            EthBlock.Block block = web3j.ethGetBlockByNumber(
//                DefaultBlockParameter.valueOf(mid), false).send().getBlock();
//            long blockTimestamp = block.getTimestamp().longValue();
//
//            if (blockTimestamp < targetTimestamp) {
//                left = mid.add(BigInteger.ONE);
//            } else if (blockTimestamp > targetTimestamp) {
//                right = mid.subtract(BigInteger.ONE);
//            } else {
//                // 找到了精确匹配的时间戳
//                return mid;
//            }
//        }
//
//        // 返回最接近的区块号
//        return right.max(BigInteger.ZERO);
//    }
//
//    /**
//     * 根据时间戳估算区块号
//     */
//    private BigInteger estimateBlockByTime(long targetTimestamp) throws IOException {
//        // 获取最新区块
//        EthBlock latestBlockResponse = web3j.ethGetBlockByNumber(
//            DefaultBlockParameter.valueOf("latest"), false).send();
//        EthBlock.Block latestBlock = latestBlockResponse.getBlock();
//        BigInteger currentBlockNumber = latestBlock.getNumber();
//        long currentTimestamp = latestBlock.getTimestamp().longValue();
//
//        // 获取创世区块时间戳
//        EthBlock.Block genesisBlock = web3j.ethGetBlockByNumber(
//            DefaultBlockParameter.valueOf(BigInteger.ZERO), false).send().getBlock();
//        long genesisTimestamp = genesisBlock.getTimestamp().longValue();
//
//        // 计算平均出块时间
//        long timeDiff = currentTimestamp - genesisTimestamp;
//        long blockDiff = currentBlockNumber.longValue();
//        double avgBlockTime = timeDiff / Math.max(blockDiff, 1.0);
//
//        // 估算目标区块号
//        long timeToTarget = targetTimestamp - genesisTimestamp;
//        BigInteger estimatedBlock = BigInteger.valueOf((long)(timeToTarget / avgBlockTime));
//
//        // 确保估算的区块号在合理范围内
//        if (estimatedBlock.compareTo(BigInteger.ZERO) < 0) {
//            return BigInteger.ZERO;
//        }
//        if (estimatedBlock.compareTo(currentBlockNumber) > 0) {
//            return currentBlockNumber;
//        }
//
//        return estimatedBlock;
//    }



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
