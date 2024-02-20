package com.example.kvdbraft.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.kvdbraft.service.RocksService;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
@Slf4j
public class RocksServiceImpl implements RocksService {
    @Resource
    RocksDB rocksDB;
    @Override
    public <T> void write(String key, T value) throws RocksDBException {
        String toJSONString = JSON.toJSONString(value);
        rocksDB.put(key.getBytes(), toJSONString.getBytes());
    }

    @Override
    public <T> T read(String key, Class<T> valueType) throws RocksDBException{
        byte[] valueBytes = rocksDB.get(key.getBytes());
        if (valueBytes != null) {
            return JSON.parseObject(valueBytes, valueType);
        }
        return null;
    }

}
