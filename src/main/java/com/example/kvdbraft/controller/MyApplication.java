package com.example.kvdbraft.controller;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.LeaderVolatileState;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.rpc.consumer.ConsumerServiceImpl;
import com.example.kvdbraft.service.ElectionService;
import com.example.kvdbraft.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MyApplication {
    @Autowired
    ConsumerServiceImpl consumerService;
    @Autowired
    Cluster cluster;
    @Autowired
    PersistenceState persistenceState;
    @Autowired
    LeaderVolatileState leaderVolatileState;
    @Autowired
    ElectionService electionService;


    @RequestMapping("/")
    @ResponseBody
    Result home1() throws RocksDBException {
        log.info("data = {}", persistenceState);
        electionService.startElection();
        return Result.success("111");

    }


}
