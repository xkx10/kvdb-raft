package com.example.kvdbraft.enums;

import com.example.kvdbraft.annotation.HeartSecurityCheck;
import com.example.kvdbraft.annotation.LogAppendSecurityCheck;
import com.example.kvdbraft.annotation.VoteSecurityCheck;

import java.lang.annotation.Annotation;

/**
 * 用于安全性检测的类型
 * @author xiaka
 */
public enum ESecurityCheckType {
    Vote(1, "投票"),
    Heart(2, "心跳"),
    LogAppend(3, "日志复制");

    public int type;
    public String message;
    ESecurityCheckType(int type, String message){
        this.type = type;
        this.message = message;
    }

}
