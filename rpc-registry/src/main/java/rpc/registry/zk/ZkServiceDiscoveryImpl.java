package rpc.registry.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import rpc.enums.RpcErrorMessageEnum;
import rpc.exception.RpcException;
import rpc.loadbalance.LoadBalance;
import rpc.protocol.RpcRequest;
import rpc.registry.ServiceDiscovery;
import rpc.registry.zk.utils.CuratorUtils;
import rpc.utils.CollectionUtil;

import java.util.List;

/**
 * 基于zookeeper的服务发现
 * 服务消费者通过这个获取一个服务对应哪些服务提供者
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private CuratorFramework zkClient= CuratorUtils.getZkClient();
    //服务发现，采用负载均衡，获取服务名称对应的所有服务提供者ip+port
    @Override
    public String lookupService(RpcRequest rpcRequest, LoadBalance loadBalance,String selfIp) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        //通过负载均衡后返回服务提供者的ip+port
        String ipPort = loadBalance.getServerBalance(selfIp, serviceUrlList);
        System.out.println(ipPort);
        return ipPort;
    }

}
