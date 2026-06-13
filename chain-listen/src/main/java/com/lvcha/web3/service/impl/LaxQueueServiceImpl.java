package com.lvcha.web3.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lvcha.web3.mapper.LaxQueueDetailMapper;
import com.lvcha.web3.pojo.LaxQueueDetail;
import com.lvcha.web3.service.LaxQueueService;
import org.springframework.stereotype.Service;

@Service
public class LaxQueueServiceImpl extends ServiceImpl<LaxQueueDetailMapper, LaxQueueDetail> implements LaxQueueService {
}
