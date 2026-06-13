package com.lvcha.web3.controller;

import com.lvcha.web3.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;

//    @GetMapping("/getUnstakeFromSn")
//    public BigInteger  getUnstakeFromSn(String address,BigInteger  sn) throws Exception {
//        BigInteger value = contractService.getUnstakeFromSn(address,sn);
//        value=value.divide(BigInteger.valueOf(1000000000000000000L));
//       return value;
//    }
//    @GetMapping("/getUlp")
//    public Tuple10 getUlp(String address,BigInteger  sn) throws Exception {
//        Tuple10 value = contractService.getUlp(address,sn);
//        return value;
//    }
//
//    @GetMapping("/getyj")
//    public ResponseEntity<Map<String, Object>> getyj(String account) {
//        BigInteger value = contractService.getyj(account);
//        value=value.divide(BigInteger.valueOf(1000000000000000000L));
//        return ResponseEntity.ok(Map.of(
//            "value", value,
//            "valueAsString", value.toString()
//        ));
//    }
//    @GetMapping("/getPid")
//    public ResponseEntity<List<String>> getPid(String account) {
//        List<String> value = contractService.getPid(account);
//        return ResponseEntity.ok(value);
//    }
//
//    @GetMapping("/getPidNeedAdd")
//    public Map<String, Object> getPidNeedAdd(String address) throws Exception {
//        return contractService.getPidNeedAdd(address);
//    }

//    @PostMapping("/dcnyStake")
//    public ResponseEntity<Map<String, Object>> dcnyStake(BigInteger amountdcny, BigInteger days) {
//        try {
//            var receipt = contractService.dcnystake(amountdcny, days);
//            return ResponseEntity.ok(Map.of(
//                "transactionHash", receipt.getTransactionHash(),
//                "blockNumber", receipt.getBlockNumber(),
//                "gasUsed", receipt.getGasUsed(),
//                "status", receipt.getStatus()
//            ));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500)
//                .body(Map.of("error", e.getMessage()));
//        }
//
//    }
//    @GetMapping("/transfer")
//    public void transferDcny() {
//        contractService.transferDcny();
//    }


//    @GetMapping("/getTodayTrans")
//    public ResponseEntity<String> getTodayTrans() throws IOException {
//        contractService.getTodayTrans();
//        return ResponseEntity.ok("value");
//    }
//    @GetMapping("/addDog")
//    public ResponseEntity<String> addDog(String address) throws IOException {
//        contractService.addDog(address);
//        return ResponseEntity.ok("success");
//    }
//
//
//    /**
//     * 部署新的SimpleStorage合约
//     * @return 新合约的地址
//     */
//    @PostMapping("/deploy")
//    public ResponseEntity<Map<String, String>> deployContract() {
//        String contractAddress = contractService.deployContract();
//        return ResponseEntity.ok(Map.of(
//            "contractAddress", contractAddress,
//            "message", "合约部署成功"
//        ));
//    }
}
