package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.service.LogService;
import com.example.kvdbraft.service.RocksService;
import lombok.extern.log4j.Log4j2;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author WangChao
 * @date 2024-02-22 21:24
 */
@Service
@Log4j2
public class LogServiceImpl implements LogService {
    private final ReentrantLock lockDB = new ReentrantLock();
    @Resource
    private PersistenceState persistenceState;
    @Resource
    private VolatileState volatileState;
    @Resource
    private RocksService rocksService;

    public void removeOnStartIndex(Integer startIndex) {
        int count = 0;
        try {
            boolean tryLock = lockDB.tryLock(3000, TimeUnit.MILLISECONDS);
            if (!tryLock) {
                throw new RuntimeException("tryLock fail, removeOnStartIndex fail");
            }
            List<Log> logs = persistenceState.getLogs();
            int lastIndex = logs.size() - 1;
            for (Integer i = startIndex; i <= lastIndex; i++) {
                // 从后开始移除
                logs.removeLast();
                count++;
            }
            volatileState.setLastIndex(lastIndex - count);
            log.info("删除成功，目前lastIndex={}", getLastIndex());
            rocksService.write("PersistenceState", persistenceState);
        } catch (InterruptedException | RocksDBException e) {
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
            persistenceState.getLogs().addAll(entry);
            volatileState.setLastIndex(getLastIndex());
            rocksService.write("PersistenceState", persistenceState);
        } catch (InterruptedException | RocksDBException e) {
            throw new RuntimeException(e);
        } finally {
            lockDB.unlock();
        }
    }

    @Override
    public Log getLastLog() {
        return persistenceState.getLogs().get(getLastIndex());
    }

    @Override
    public List<Log> getNoApplyLogList() {
        Integer commitIndex = volatileState.getCommitIndex();
        Integer lastApplied = volatileState.getLastApplied();
        List<Log> applyLogs = persistenceState.getLogs().subList(lastApplied + 1, commitIndex + 1);
        return applyLogs;
    }

    private Integer getLastIndex() {
        return persistenceState.getLogs().size() - 1;
    }
}
