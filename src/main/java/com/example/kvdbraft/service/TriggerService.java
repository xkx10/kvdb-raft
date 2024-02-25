package com.example.kvdbraft.service;

public interface TriggerService {
    /**
     * 开启超时选举任务
     */
    void startElectionTask();

    /**
     * 停止超时选举任务
     */
    void stopElectionTask();

    /**
     * 刷新超时选举时间
     * 比如收到心跳等情况
     */
    void updateElectionTaskTime();

    /**
     * 开始心跳任务
     */
    void startHeartTask();

    /**
     * 停止心跳任务
     */
    void stopHeartTask();

}
