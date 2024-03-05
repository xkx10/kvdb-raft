package com.example.kvdbraft.controller;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.LeaderVolatileState;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.rpc.consumer.ConsumerServiceImpl;
import com.example.kvdbraft.service.ClientOperationService;
import com.example.kvdbraft.service.ElectionService;
import com.example.kvdbraft.service.HeartbeatService;
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.service.impl.client.handler.ReadStrategy;
import com.example.kvdbraft.service.impl.redis.RedisClient;
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
    SecurityCheckService service;
    @Resource
    private ClientOperationService clientOperationService;

    @Resource
    private HeartbeatService heartbeatService;
    @Resource
    RedisClient redisClient;

    @Resource
    ReadStrategy readStrategy;
    @RequestMapping("/")
    @ResponseBody
    Result home1() throws RocksDBException {
        Boolean data = clientOperationService.execute("set name11 xkx11");
        if(!data) throw new RuntimeException();
        return Result.success("ok");
    }

    @RequestMapping("/heartNotJudgeResult")
    @ResponseBody
    Result home2() throws RocksDBException {
        log.info("data = {}", persistenceState);
        //  初始化所有状态
        List<Future<Boolean>> futures = heartbeatService.heartNotJudgeResult();
        return Result.success("111");
    }


    @RequestMapping("/write")
    @ResponseBody
    Result<String> write(String command){
        Boolean data = clientOperationService.execute(command);
        return data? Result.success("success"):Result.failure("false");
    }
    @RequestMapping("/read")
    @ResponseBody
    Result<String> read(String command){
        String data = readStrategy.execute(command);
        return Result.success(data);
    }
    @RequestMapping("/jmeter")
    @ResponseBody
    Result jmeter() throws RocksDBException {

        String data = readStrategy.execute("get name11");
        return Result.success(data);
    }

}
