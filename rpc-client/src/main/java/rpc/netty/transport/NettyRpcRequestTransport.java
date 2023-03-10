package rpc.netty.transport;

import rpc.loadbalance.LoadBalance;
import rpc.protocol.RpcRequest;
import rpc.registry.ServiceDiscovery;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author yhw
 * @version 1.0
 **/
public class NettyRpcRequestTransport implements RpcRequestTransport {
    //负载均衡器
    private LoadBalance loadBalance;

    //服务发现
    private ServiceDiscovery serviceDiscovery;

    /**
     *
     * @param balance  采用哪种负载均衡
     */
    public NettyRpcRequestTransport(LoadBalance balance,ServiceDiscovery serviceDiscovery){
        this.loadBalance = balance;
        this.serviceDiscovery = serviceDiscovery;
    }
    @Override
    public String sendRpcRequest(RpcRequest rpcRequest) {
        try {
            String serviceIpPort = serviceDiscovery.lookupService(rpcRequest, loadBalance, InetAddress.getLocalHost().getHostAddress());
            return serviceIpPort;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
