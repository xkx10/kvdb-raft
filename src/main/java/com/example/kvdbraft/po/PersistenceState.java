package com.example.kvdbraft.po;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
@Data
public class PersistenceState {
    Integer currentTerm;
    String votedFor;
    List<Log> logs;

    @Autowired
    RocksDB rocksDB;

    private PersistenceState(){
        try {
            byte[] persistenceStateBytes = rocksDB.get("PersistenceState".getBytes());
            if(persistenceStateBytes == null){
                currentTerm = 0;
                votedFor = null;
                logs = new ArrayList<>();
                return;
            }
            PersistenceState persistenceState = JSON.parseObject(persistenceStateBytes, PersistenceState.class);
            BeanUtils.copyProperties(instance, persistenceState);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    };
    private volatile static PersistenceState instance;
    public static PersistenceState getInstance(){
        if(instance == null){
            synchronized (PersistenceState.class){
                if(instance == null){
                    instance = new PersistenceState();
                }
            }
        }
        return instance;
    }
}
