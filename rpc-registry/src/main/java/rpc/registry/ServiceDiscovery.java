package rpc.registry;



import rpc.loadbalance.LoadBalance;
import rpc.protocol.RpcRequest;

import java.net.InetSocketAddress;
import java.util.List;

public interface ServiceDiscovery {
    /**
     *
     * @param rpcRequest  请求
     * @param loadBalance 采用哪种负载均衡器
     * @param selfIp      运行的ip地址
     * @return
     */
    String lookupService(RpcRequest rpcRequest, LoadBalance loadBalance,String selfIp);
}
