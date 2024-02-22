package com.example.kvdbraft.controller;

import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.LeaderVolatileState;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.rpc.consumer.ConsumerServiceImpl;
import com.example.kvdbraft.service.ElectionService;
import com.example.kvdbraft.service.HeartbeatService;
import com.example.kvdbraft.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.Future;

@RestController
@Slf4j
public class MyApplication {
    @Resource
    ConsumerServiceImpl consumerService;
    @Resource
    Cluster cluster;
    @Resource
    PersistenceState persistenceState;
    @Resource
    LeaderVolatileState leaderVolatileState;
    @Resource
    ElectionService electionService;

    @Resource
    private HeartbeatService heartbeatService;


    @RequestMapping("/")
    @ResponseBody
    Result home1() throws RocksDBException {
        log.info("data = {}", persistenceState);
        //  初始化所有状态
        electionService.startElection();
        return Result.success("111");
    }

    @RequestMapping("/heartNotJudgeResult")
    @ResponseBody
    Result home2() throws RocksDBException {
        log.info("data = {}", persistenceState);
        //  初始化所有状态
        List<Future<Boolean>> futures = heartbeatService.heartNotJudgeResult();
        return Result.success("111");
    }
}
