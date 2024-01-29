package com.example.kvdbraft.po.cache;

import com.alibaba.fastjson.JSON;
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
    private Long commitIndex;
    private Long lastApplied;
    @Autowired
    private RocksDB rocksDB;
    @PostConstruct
    public void init() {
        try {
            byte[] volatileStates = rocksDB.get("VolatileState".getBytes());
            if(volatileStates == null){
                commitIndex = -1L;
                lastApplied = -1L;
                return;
            }
            VolatileState volatileState = JSON.parseObject(volatileStates, VolatileState.class);
            BeanUtils.copyProperties(volatileState, this);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
}
