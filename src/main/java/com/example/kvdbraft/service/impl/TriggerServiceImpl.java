package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.NodeConfigField;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.service.ElectionService;
import com.example.kvdbraft.service.HeartbeatService;
import com.example.kvdbraft.service.TriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TriggerServiceImpl implements TriggerService {
    /**
     *
     */
    private ScheduledThreadPoolExecutor heartTaskExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledThreadPoolExecutor electionTaskExecutor = new ScheduledThreadPoolExecutor(1);
    @Resource
    NodeConfigField nodeConfigField;
    private int randomTimeRange = 100;

    private final Random random = new Random();
    @Resource
    ElectionService electionService;
    @Resource
    HeartbeatService heartbeatService;
    @Resource
    VolatileState volatileState;

    private ScheduledFuture<?> electionTaskFuture;
    private ScheduledFuture<?> heartTaskFuture;

    private long electionDelayTime = 15*1000;

    @PostConstruct
    public void init(){
        startElectionTaskByTime(electionDelayTime);
    }

    @Override
    public void startElectionTask() {
        // 初始化超时选举任务
        startElectionTaskByTime(nodeConfigField.getElectionTimeout());

    }

    @Override
    public void stopElectionTask() {
        if (electionTaskFuture != null) {
            electionTaskFuture.cancel(false);
        }
    }

    @Override
    public void updateElectionTaskTime() {
        synchronized (TriggerService.class){
            // 取消当前任务，但不中断正在执行的任务
            stopElectionTask();
            // 重新调度任务
            startElectionTask();
        }
    }

    @Override
    public void startHeartTask() {
        log.info("Starting heart task... ");
        heartTaskFuture = heartTaskExecutor.scheduleAtFixedRate(() -> heartbeatService.heartNotJudgeResult(), 0, nodeConfigField.getHeartbeatIntervalTime(), TimeUnit.MILLISECONDS);
        log.info("Heart task started successfully.");
    }

    @Override
    public void stopHeartTask() {
        if (heartTaskFuture != null) {
            heartTaskFuture.cancel(false);
        }
    }
    public void startElectionTaskByTime(long time) {
        // 初始化超时选举任务
        int randomTime = random.nextInt(randomTimeRange);
        electionTaskFuture = electionTaskExecutor.schedule(() -> {
            boolean b = electionService.startElection();
            if(!b){
                startElectionTask();
            }
        }, time + randomTime, TimeUnit.MILLISECONDS);

    }

}
