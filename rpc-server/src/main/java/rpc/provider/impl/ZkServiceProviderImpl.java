package rpc.provider.impl;

import lombok.extern.slf4j.Slf4j;
import rpc.enums.RpcErrorMessageEnum;
import rpc.exception.RpcException;
import rpc.provider.ServiceProvider;
import rpc.registry.ServiceRegistry;
import rpc.registry.zk.ZkServiceRegistryImpl;
import rpc.utils.ReflectionUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shuang.kou
 * @createTime 2020年05月13日 11:23:00
 */
@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    //服务提供者一方面要向服务注册中心注册服务，同时自己本身也要存储使用单例模式，这样子才能调用
    /**
     * key: 全类名
     * value: service object
     */
    private final Map<String, Object> serviceMap;
    //向服务注册中心注册的类
    private final ServiceRegistry serviceRegistry;
    private final InetSocketAddress InetSocketAddress;
    public ZkServiceProviderImpl(String ip,int port) {
        serviceMap = new ConcurrentHashMap<>();
        serviceRegistry = new ZkServiceRegistryImpl();
        InetSocketAddress = new InetSocketAddress(ip,port);
    }



    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public void addService(String interfaceName, Object obj) {
        //本地存根
        serviceMap.put(interfaceName,obj);

        //注册到服务中心
        serviceRegistry.registerService(interfaceName, InetSocketAddress);
    }


}
