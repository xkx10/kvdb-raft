package com.example.kvdbraft.po;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author WangChao
 * @date 2024-02-04 11:42
 */
@Data
@Component
@ConfigurationProperties(prefix = "node.config.field")
public class NodeConfigField {
    private long shortLeaseTerm;
    private long heartbeatIntervalTime;
    private long electionTimeout;

    // 上一次租期时间戳
    private long lastShortLeaseTerm;

    private int shardSize;
}
