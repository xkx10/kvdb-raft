package com.example.kvdbraft.service;

import com.example.kvdbraft.po.Log;

import java.util.List;

/**
 * @author WangChao
 * @date 2024-02-22 21:23
 */
public interface LogService {
    public void removeOnStartIndex(Integer startIndex);

    void writeLog(List<Log> entry);

    /**
     * 获取最后一条log日志
     * @return
     */
    Log getLastLog();

    /**
     * 获取未被apply的logList
     * @return
     */
    List<Log> getNoApplyLogList();
}
