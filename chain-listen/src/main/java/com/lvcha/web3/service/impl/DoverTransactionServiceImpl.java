package com.lvcha.web3.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lvcha.web3.mapper.DoverTransactionDetailMapper;
import com.lvcha.web3.pojo.DoverTransactionDetail;
import com.lvcha.web3.service.DoverTransactionService;
import org.springframework.stereotype.Service;

@Service
public class DoverTransactionServiceImpl extends ServiceImpl<DoverTransactionDetailMapper, DoverTransactionDetail> implements DoverTransactionService {
}
