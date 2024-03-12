package com.example.kvdbraft.po.cache;

import com.example.kvdbraft.enums.EPersistenceKeys;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.service.impl.redis.RedisClient;
import io.lettuce.core.RedisException;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.rocksdb.RocksDBException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * RocksDB持久化节点配置
 */
@Component
@Data
public class PersistenceState {
    private Long currentTerm;
    private String votedFor;

    @Resource
    private RedisClient redisClient;

    @PostConstruct
    public void init() {
        try {
            Integer PerCurrentTerm = (Integer) redisClient.get(EPersistenceKeys.PerCurrentTerm.key);
            Object PerVotedFor = redisClient.get(EPersistenceKeys.PerVotedFor.key);
            this.currentTerm = PerCurrentTerm == null ? 0L : PerCurrentTerm.longValue();
            this.votedFor = PerVotedFor == null ? null : (String) PerVotedFor;
            redisClient.set(EPersistenceKeys.PerCurrentTerm.key, this.currentTerm);
            redisClient.set(EPersistenceKeys.PerVotedFor.key, this.votedFor);
            if(redisClient.getKeysByPrefix(EPersistenceKeys.LogEntries.key).isEmpty()){
                Log log = Log.builder().index(0).term(-1L).build();
                redisClient.lSetByShard(EPersistenceKeys.LogEntries.key, log, 0);
            }
        } catch (RedisException e) {
            throw new RuntimeException(e);
        }
    }

    public void increaseTerm() {
        synchronized (PersistenceState.class) {
            currentTerm = currentTerm + 1;
        }
    }

    public void setCurrentTerm(long currentTerm) {
        synchronized (PersistenceState.class) {
            this.currentTerm = currentTerm;
            redisClient.set(EPersistenceKeys.PerCurrentTerm.key, this.currentTerm);
        }
    }

    public void setVotedFor(String votedFor) {
        synchronized (PersistenceState.class) {
            this.votedFor = votedFor;
            redisClient.set(EPersistenceKeys.PerVotedFor.key, this.votedFor);
        }
    }
}
