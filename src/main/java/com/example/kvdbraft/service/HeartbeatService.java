package com.example.kvdbraft.service;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.vo.Result;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author WangChao
 * @date 2024-02-04 9:11
 */
public interface HeartbeatService {
    /**
     * 发送心跳
     * @return 跟随者回应记录
     */
    List<Future<Boolean>> heartNotJudgeResult();


    AppendEntriesResponseDTO handlerHeart(AppendEntriesDTO heartDTO);
}
