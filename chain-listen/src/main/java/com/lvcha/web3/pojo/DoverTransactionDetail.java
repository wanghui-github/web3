package com.lvcha.web3.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("dover_transaction_detail")
public class DoverTransactionDetail {
    private String transactionHash;
    private String fromAddress;
    private String toAddress;
    private Integer amount;
    private Integer days;
    private Integer timestamp;
    private Integer blockNumber;
    private String method;
    private Integer isKy;
    private String  operation;
    private Integer sn;
}

