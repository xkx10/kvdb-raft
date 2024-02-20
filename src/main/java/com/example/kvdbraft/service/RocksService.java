package com.example.kvdbraft.service;

import org.rocksdb.RocksDBException;

public interface RocksService {
    /**
     * 写入数据
     * @param key key
     * @param value 写入值
     * @throws RocksDBException 写入异常
     */
    <T> void write(String key, T value) throws RocksDBException;

    /**
     * 读取数据
     * @param key key
     * @param valueType 反序列化后的类型
     * @return value
     * @throws RocksDBException 读取异常
     */
    <T> T read(String key, Class<T> valueType) throws RocksDBException;

}
