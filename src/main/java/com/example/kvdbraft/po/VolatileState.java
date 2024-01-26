package com.example.kvdbraft.po;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class VolatileState {
    private Long commitIndex;
    private Long lastApplied;
    @Autowired
    RocksDB rocksDB;
    private VolatileState(){
        try {
            byte[] volatileStates = rocksDB.get("VolatileState".getBytes());
            if(volatileStates == null){
                commitIndex = -1L;
                lastApplied = -1L;
                return;
            }
            VolatileState volatileState = JSON.parseObject(volatileStates, VolatileState.class);
            BeanUtils.copyProperties(volatileState, instance);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
    private static volatile VolatileState instance;
    public static VolatileState getInstance(){
        if(instance == null){
            synchronized (VolatileState.class){
                if(instance == null){
                    instance = new VolatileState();
                }
            }
        }
        return instance;
    }
}
