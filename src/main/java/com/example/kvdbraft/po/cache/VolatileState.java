package com.example.kvdbraft.po.cache;

import com.alibaba.fastjson.JSON;
import com.example.kvdbraft.enums.EStatus;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Data
public class VolatileState {
    // 已经提交到logs中的最大下标
    private Integer commitIndex;
    private Integer lastApplied;
    private Integer lastIndex;
    // 身份标识
    private Integer status;
    private String leaderId;
    @Autowired
    private RocksDB rocksDB;

    @PostConstruct
    public void init() {
        try {
            byte[] volatileStates = rocksDB.get("VolatileState".getBytes());
            if (volatileStates == null) {
                commitIndex = -1;
                lastApplied = -1;
                lastIndex = -1;
                return;
            }
            VolatileState volatileState = JSON.parseObject(volatileStates, VolatileState.class);
            BeanUtils.copyProperties(volatileState, this);
            status = EStatus.Follower.status;
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
}
