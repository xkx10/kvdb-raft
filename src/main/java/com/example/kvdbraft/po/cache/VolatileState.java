package com.example.kvdbraft.po.cache;

import com.alibaba.fastjson.JSON;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.service.RocksService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Data
public class VolatileState {
    private Integer commitIndex;
    private Integer lastApplied;
    private Integer lastIndex;
    // 身份标识
    private Integer status;
    private String leaderId;
    @Resource
    private RocksService rocksService;

    @PostConstruct
    public void init() {
        try {
            VolatileState volatileState = rocksService.read("VolatileState", VolatileState.class);
            if (volatileState == null) {
                commitIndex = 0;
                lastApplied = 0;
                lastIndex = 0;
                return;
            }
            BeanUtils.copyProperties(volatileState, this);
            status = EStatus.Follower.status;
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
}
