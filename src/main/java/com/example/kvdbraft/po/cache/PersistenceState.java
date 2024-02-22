package com.example.kvdbraft.po.cache;

import com.alibaba.fastjson.JSON;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.Log.LogBuilder;
import com.example.kvdbraft.service.RocksService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
    @Resource
    private RocksService rocksService;

    @PostConstruct
    public void init() {
        try {
            PersistenceState persistenceState = rocksService.read("PersistenceState", PersistenceState.class);
            if(persistenceState == null){
                currentTerm = 0L;
                votedFor = null;
                logs = new ArrayList<>();
                Log log = Log.builder().index(0).term(-1L).build();
                logs.add(log);
                return;
            }
            BeanUtils.copyProperties(persistenceState, this);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
}
