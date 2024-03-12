package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.enums.EPersistenceKeys;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.service.LogService;
import com.example.kvdbraft.service.impl.redis.RedisClient;
import lombok.extern.log4j.Log4j2;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author WangChao
 * @date 2024-02-22 21:24
 */
@Service
@Log4j2
public class LogServiceImpl implements LogService {
    private final ReentrantLock lockDB = new ReentrantLock();
    @Resource
    private VolatileState volatileState;
    @Resource
    private RedisClient redisClient;

    public void removeOnStartIndex(Integer startIndex) {
        try {
            boolean tryLock = lockDB.tryLock(3000, TimeUnit.MILLISECONDS);
            if (!tryLock) {
                throw new RuntimeException("tryLock fail, removeOnStartIndex fail");
            }
            redisClient.lRemoveShardFromLast(EPersistenceKeys.LogEntries.key, startIndex);
            int size = redisClient.lGetByShardSize(EPersistenceKeys.LogEntries.key).intValue();
            volatileState.setLastIndex(size - 1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lockDB.unlock();
        }
    }

    @Override
    public void writeLog(List<Log> entry) {
        try {
            boolean tryLock = lockDB.tryLock(3000, TimeUnit.MILLISECONDS);
            if (!tryLock) {
                throw new RuntimeException("tryLock fail, writeLog fail");
            }
            int index = getLastIndex() + 1;
            for (int i = index; i < index + entry.size(); i++) {
                redisClient.lSetByShard(EPersistenceKeys.LogEntries.key, entry.get(i - index), i);
            }
            volatileState.setLastIndex(getLastIndex());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lockDB.unlock();
        }
    }

    @Override
    public Log getLastLog() {
        return (Log) redisClient.lGetByShardIndex(EPersistenceKeys.LogEntries.key, getLastIndex());
    }

    @Override
    public List<Log> getNoApplyLogList() {
        Integer commitIndex = volatileState.getCommitIndex();
        Integer lastApplied = volatileState.getLastApplied();
        List<Log> res = new ArrayList<>();
        redisClient.lGetByShardIndexAfter(EPersistenceKeys.LogEntries.key, lastApplied + 1)
                .forEach(e -> res.add((Log) e));
        return res.subList(0, commitIndex - lastApplied);
    }

    public Integer getLastIndex() {
        return (redisClient.lGetByShardSize(EPersistenceKeys.LogEntries.key).intValue() - 1);
    }
}
