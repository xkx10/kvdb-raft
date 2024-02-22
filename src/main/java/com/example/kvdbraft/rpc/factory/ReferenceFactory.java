package com.example.kvdbraft.rpc.factory;

import com.example.kvdbraft.rpc.interfaces.ProviderService;
import org.apache.dubbo.config.ReferenceConfig;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author WangChao
 * @date 2024-02-04 9:18
 */
public class ReferenceFactory {
    private static final Map<String, ProviderService> rpcMap = new ConcurrentHashMap<>();

    private static final String rpcVersion = "1.0";

    /**
     * 动态ip直连RPC 创建引用
     *
     * @param url            调用地址
     * @param interfaceClass 参数类型
     * @param <T>            泛型
     * @return 返回结果
     */
    private static <T> T createRpcReference(String url, Class<T> interfaceClass) {
        T rpcService = null;
        try {
            // ReferenceConfigCache会在内部进行连接配置缓存
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
     *
     * @param url 调用地址
     * @return 放回结果
     */
    public static ProviderService getOrCreateReference(String url) {
        return rpcMap.computeIfAbsent(url, key -> createRpcReference(key, ProviderService.class));
    }
}
