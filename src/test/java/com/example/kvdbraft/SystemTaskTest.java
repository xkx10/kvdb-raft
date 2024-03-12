package com.example.kvdbraft;

import com.example.kvdbraft.service.impl.redis.RedisClient;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SystemTaskTest {

    @Resource
    private RedisClient redisClient;

    @Test
    public void updateHour(){
        redisClient.executeRedisCommand("set name xkx");
        System.out.println(redisClient.get("name"));
    }

    @Test
    public void test(){
        System.out.println("key-0".compareTo("key-1"));
    }
}
