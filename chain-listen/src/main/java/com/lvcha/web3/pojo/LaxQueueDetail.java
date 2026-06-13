package com.lvcha.web3.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
@TableName("lax_queue_detail")
public class LaxQueueDetail {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String address;
    private Integer queueIndex;
    private BigInteger amount;
    private Long queueTime;
    private Integer type;
    private String createDate;
    private Date createTime;
}
