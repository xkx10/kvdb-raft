package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.service.impl.redis.RedisClient;
import com.example.kvdbraft.service.LogService;
import com.example.kvdbraft.service.StateMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RedisStateMachineServiceImpl implements StateMachineService {
    @Resource
    LogService logService;
    @Resource
    RedisClient redisClient;

    @Resource
    VolatileState volatileState;
    @Override
    public void apply() {
        synchronized (RedisStateMachineServiceImpl.class){
            List<Log> applyLogList = logService.getNoApplyLogList();
            log.info("applyLogList = {}", applyLogList);
            for (Log log : applyLogList) {
                redisClient.set(log.getCommand().getKey(), log.getCommand().getValue());
            }
            volatileState.setLastApplied(volatileState.getLastApplied() + applyLogList.size());
        }
    }

}
