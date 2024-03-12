package com.example.kvdbraft.enums;

/**
 * 用于安全性检测的类型
 *
 * @author xiaka
 */
public enum EPersistenceKeys {
    LogEntries("logEntries"),
    PerCurrentTerm("PersistenceState-currentTerm"),
    PerVotedFor("PersistenceState-votedFor"),
    VolCommitIndex("VolatileState-commitIndex"),
    VolLastApplied("VolatileState-lastApplied"),
    VolLastIndex("VolatileState-lastIndex");

    public String key;

    EPersistenceKeys(String key) {
        this.key = key;
    }
}
