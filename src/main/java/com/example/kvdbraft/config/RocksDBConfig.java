package com.example.kvdbraft.config;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class RocksDBConfig {
    private RocksDB rocksDB;
    @Value("${rocksdb.dbDir}")
    private String dbDir;
    @Value("${dubbo.application.name}")
    private String name;


    @PostConstruct
    public void init() {
        dbDir = dbDir + name;
        RocksDB.loadLibrary();
        File file = new File(dbDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            rocksDB = RocksDB.open(options, dbDir);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void closeRocksDB() {
        if (rocksDB != null) {
            rocksDB.close();
        }
    }

    @Bean
    public RocksDB rocksDB() {
        return rocksDB;
    }
}
