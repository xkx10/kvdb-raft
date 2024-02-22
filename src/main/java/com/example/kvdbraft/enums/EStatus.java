package com.example.kvdbraft.enums;

/**
 * 节点身份
 * @author xiaka
 */
public enum EStatus {
    Leader(1, "领导人(主节点)"),
    Candidate(2, "候选人"),
    Follower(3, "跟随着");
    public int status;
    public String name;
    EStatus(int status, String name){
        this.status = status;
        this.name = name;
    }

}
