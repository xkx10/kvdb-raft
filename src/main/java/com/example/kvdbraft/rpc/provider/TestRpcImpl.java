package com.example.kvdbraft.rpc.provider;

import com.example.kvdbraft.rpc.interfaces.TestRpc;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(group = "${server.port}",version = "1.0")
public class TestRpcImpl implements TestRpc {
    @Override
    public void testHallow() {
        System.out.println("hello1");
    }

}
