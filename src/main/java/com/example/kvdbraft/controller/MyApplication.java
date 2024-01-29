package com.example.kvdbraft.controller;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.po.Cluster;
import com.example.kvdbraft.rpc.consumer.ConsumerServiceImpl;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import com.example.kvdbraft.vo.Result;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class MyApplication {
    @Autowired
    ConsumerServiceImpl consumerService;

    @RequestMapping("/")
    @ResponseBody
    Result home1() throws RocksDBException {


        return consumerService.sendElection("dubbo://localhost:9012",new RequestVoteDTO());

    }


}
