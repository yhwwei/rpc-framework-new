package rpc.test.client;

import rpc.loadbalance.ConsistentHashingWithVirtualNode;
import rpc.netty.NettyRpcClient;
import rpc.netty.transport.NettyRpcRequestTransport;
import rpc.netty.transport.RpcRequestTransport;
import rpc.proxy.RpcClientProxy;
import rpc.registry.ServiceDiscovery;
import rpc.registry.zk.ZkServiceDiscoveryImpl;
import rpc.test.service.Hello;

/**
 * @author yhw
 * @version 1.0
 **/
public class ClientMain {
    public static void main(String[] args) {
        ConsistentHashingWithVirtualNode consistentHashingWithVirtualNode = new ConsistentHashingWithVirtualNode();
        ServiceDiscovery serviceDiscovery = new ZkServiceDiscoveryImpl();
        RpcRequestTransport rpcRequestTransport = new NettyRpcRequestTransport(consistentHashingWithVirtualNode,serviceDiscovery);
        NettyRpcClient nettyRpcClient = new NettyRpcClient(rpcRequestTransport);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(nettyRpcClient);
        Hello proxy = rpcClientProxy.getProxy(Hello.class);
        System.out.println(proxy.hello("1111"));

    }
}
