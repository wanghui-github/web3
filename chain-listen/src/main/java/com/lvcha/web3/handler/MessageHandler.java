package com.lvcha.web3.handler;

import com.lvcha.web3.pojo.DoverTransactionDetail;
import com.lvcha.web3.service.ContractService;
import com.lvcha.web3.service.DoverTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.Map;

@Service
@Slf4j
public  class MessageHandler {

    @Resource
    private DoverTransactionService doverTransactionService;
    @Resource
    private ContractService contractService;

    public  void handleMessage(Map map)  {

        Map<String, Object> txMap = (Map<String, Object>) map.get("tx");
        Map<String, Object> blockMap = (Map<String, Object>) map.get("block");

        DoverTransactionDetail detail = parseTransactionFromMap(txMap, blockMap);
        log.info("{}", detail);
        try {
            getDetailsFromSnAndSave(detail);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Async
    public void getDetailsFromSnAndSave(DoverTransactionDetail detail) throws Exception {
        if(detail==null){
            return;
        }
        if(detail.getSn()!=null){
            //根据sn读取金额
            BigInteger amount = contractService.getUnstakeFromSn(detail.getFromAddress(), BigInteger.valueOf(detail.getSn()));
            detail.setAmount(amount.divide(BigInteger.valueOf(1000000000000000000L)).intValue());
        }
        doverTransactionService.save(detail);
    }


    public DoverTransactionDetail parseTransactionFromMap(Map<String, Object> tx, Map<String, Object> block) {
        try {
            String input = (String) tx.get("input");

            // Extract function parameters from transaction input
            if (input != null && input.length() >= 10) {
                String method = input.substring(0, 10);
                // stake
                if ("0x7e79d4b9".equals(method)) {
                    String paramData = input.substring(10);
                    String amountHex = paramData.substring(0, 64);
                    String daysHex = paramData.substring(64, 128);
                    String isKyHex = paramData.substring(128, 192);

                    BigInteger amount = new BigInteger(amountHex, 16);
                    BigInteger days = new BigInteger(daysHex, 16);
                    BigInteger isKy = new BigInteger(isKyHex, 16);

                    BigInteger originalAmount = amount.divide(BigInteger.valueOf(1_000_000_000_000_000_000L));
                    DoverTransactionDetail detail = new DoverTransactionDetail();
                    detail.setTransactionHash((String) tx.get("hash"));
                    detail.setFromAddress((String) tx.get("from"));
                    detail.setToAddress((String) tx.get("to"));
                    detail.setAmount(originalAmount.intValue());
                    detail.setDays(days.intValue());
                    detail.setTimestamp((Integer) block.get("timestamp"));
                    detail.setBlockNumber((Integer) block.get("number"));
                    detail.setMethod(method);
                    detail.setIsKy(isKy.intValue());
                    detail.setOperation("1");
                    return detail;

                } else if ("0x050d05fd".equals(method)||"0x2e17de78".equals(method)) {//kydcnyunstake
                    String paramData = input.substring(10);
                    BigInteger sn = new BigInteger(paramData, 16);
                    DoverTransactionDetail detail = new DoverTransactionDetail();
                    detail.setTransactionHash((String) tx.get("hash"));
                    detail.setFromAddress((String) tx.get("from"));
                    detail.setToAddress((String) tx.get("to"));
                    detail.setTimestamp((Integer) block.get("timestamp"));
                    detail.setBlockNumber((Integer) block.get("number"));
                    detail.setMethod(method);
                    detail.setOperation("-1");
                    detail.setSn(sn.intValue());
                    return detail;
                }else if ("0x38ed1739".equals(method)){ //swap交易
                    DoverTransactionDetail detail = new DoverTransactionDetail();
                    detail.setFromAddress((String) tx.get("from"));
                    detail.setToAddress((String) tx.get("to"));
                    detail.setTransactionHash((String) tx.get("hash"));
                    detail.setFromAddress((String) tx.get("from"));
                    detail.setToAddress((String) tx.get("to"));
                    detail.setTimestamp((Integer) block.get("timestamp"));
                    detail.setBlockNumber((Integer) block.get("number"));
                    detail.setMethod(method);
                    try {
                        contractService.addDog(detail.getFromAddress());
                        detail.setOperation("dog-success");
                    }catch (Exception e){
                        log.info("Error addDog: {}", (String) tx.get("from"), e);
                        detail.setOperation("dog-fail");
                    }

                }

            }

        } catch (Exception e) {
            log.error("Error parsing dcnystake transaction: {}", (String) tx.get("hash"), e);
        }

        return null;
    }

    public static void main(String[] args) {
        // 使用Web3j工具计算
//        String methodSignature = "kydcnyunstake(uint256)";
//        byte[] hashed = Hash.sha3(methodSignature.getBytes());
//        String methodId = Numeric.toHexString(hashed).substring(0, 10); // 包含0x前缀
//        System.out.println(methodId);
        String param="0000000000000000000000000000000000000000000000000000000000003f00";
        BigInteger sn = new BigInteger(param, 16);
        System.out.println(sn);
    }

}
