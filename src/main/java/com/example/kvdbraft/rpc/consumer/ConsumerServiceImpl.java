package com.example.kvdbraft.rpc.consumer;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import com.example.kvdbraft.vo.Result;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConsumerServiceImpl implements ConsumerService {
    private Map<String, ProviderService> rpcMap = new ConcurrentHashMap<>();

    private String rpcVersion = "1.0";

    @Override
    public Result sendElection(String url, RequestVoteDTO requestVoteDTO) {
        try {
            ProviderService providerService = getOrCreateReference(url);
            return providerService.handlerElection(requestVoteDTO);
        } catch (RpcException rpcException) {
            // RPC连接创建后发送请求出现异常（一般情况是对面机器宕机了）
            return Result.failure("RPC请求失败：" + rpcException.getMessage());
        } catch (Exception e) {
            // 其他异常
            return Result.failure("发生未知错误：" + e.getMessage());
        }
    }

    /**
     * 动态ip直连RPC 创建引用
     * @param url
     * @param interfaceClass
     * @return
     * @param <T>
     */
    private  <T> T createRpcReference(String url, Class<T> interfaceClass) {
        T rpcService = null;
        try {
            ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
            referenceConfig.setInterface(interfaceClass);
            referenceConfig.setUrl(url);
            referenceConfig.setVersion(rpcVersion);
            rpcService = referenceConfig.get();
        } catch (Exception e) {
            System.err.println("创建 RPC 引用时发生异常：" + e.getMessage());

        }
        return rpcService;
    }

    /**
     * 从缓存中获取对应的RPC Reference，或者创建Reference
     * @param url
     * @return
     */
    private ProviderService getOrCreateReference(String url) {
        return rpcMap.computeIfAbsent(url, key -> createRpcReference(key, ProviderService.class));
    }
}
