package com.example.kvdbraft.po.cache;

import com.alibaba.fastjson.JSON;
import com.example.kvdbraft.po.Log;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RocksDB持久化节点配置
 *
 */
@Component
@Data
public class PersistenceState {
    private Long currentTerm;
    private String votedFor;
    private List<Log> logs;
    @Autowired
    private RocksDB rocksDB;

    @PostConstruct
    public void init() {
        try {
            byte[] persistenceStateBytes = rocksDB.get("PersistenceState".getBytes());
            if(persistenceStateBytes == null){
                currentTerm = 0L;
                votedFor = null;
                logs = new ArrayList<>();
                return;
            }
            PersistenceState persistenceState = JSON.parseObject(persistenceStateBytes, PersistenceState.class);
            BeanUtils.copyProperties(persistenceState, this);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
}
