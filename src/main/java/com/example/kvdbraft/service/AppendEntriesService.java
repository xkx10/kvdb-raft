package com.example.kvdbraft.service;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.vo.Result;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author WangChao
 * @date 2024-02-22 19:45
 */
public interface AppendEntriesService {
    /**
     * 主节点发送日志
     *
     * @return 返回结果
     */
    Boolean sendLogToFollow(Log log);


    /**
     * 接收日志
     *
     * @param entriesDTO 接收日志体
     * @return 返回结果
     */
    AppendEntriesResponseDTO appendEntries(AppendEntriesDTO entriesDTO);
}
