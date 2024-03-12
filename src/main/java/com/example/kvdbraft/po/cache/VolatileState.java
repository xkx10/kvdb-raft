package com.example.kvdbraft.po.cache;

import com.example.kvdbraft.enums.EPersistenceKeys;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.service.impl.redis.RedisClient;
import io.lettuce.core.RedisException;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.rocksdb.RocksDBException;
import org.springframework.beans.BeanUtils;
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
    private RedisClient redisClient;

    @PostConstruct
    public void init() {
        try {
            Object VolCommitIndex = redisClient.get(EPersistenceKeys.VolCommitIndex.key);
            Object VolLastApplied = redisClient.get(EPersistenceKeys.VolLastApplied.key);
            Object VolLastIndex = redisClient.get(EPersistenceKeys.VolLastIndex.key);
            this.commitIndex = VolCommitIndex == null ? 0 : (Integer) VolCommitIndex;
            this.lastApplied = VolLastApplied == null ? 0 : (Integer) VolLastApplied;
            this.lastIndex = VolLastIndex == null ? 0 : (Integer) VolLastIndex;
            redisClient.set(EPersistenceKeys.VolCommitIndex.key, commitIndex);
            redisClient.set(EPersistenceKeys.VolLastApplied.key, lastApplied);
            redisClient.set(EPersistenceKeys.VolLastIndex.key, lastIndex);
            status = EStatus.Follower.status;
        } catch (RedisException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
        redisClient.set(EPersistenceKeys.VolCommitIndex.key, commitIndex);
    }

    public void setLastApplied(int lastApplied) {
        this.lastApplied = lastApplied;
        redisClient.set(EPersistenceKeys.VolLastApplied.key, lastApplied);
    }

    public void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
        redisClient.set(EPersistenceKeys.VolLastIndex.key, lastIndex);
    }
}
