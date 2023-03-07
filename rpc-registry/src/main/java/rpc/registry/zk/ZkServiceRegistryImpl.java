package rpc.registry.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import rpc.registry.ServiceRegistry;
import rpc.registry.zk.utils.CuratorUtils;

import java.net.InetSocketAddress;


@Slf4j
public class ZkServiceRegistryImpl implements ServiceRegistry {
    private CuratorFramework zkClient = CuratorUtils.getZkClient();

    //服务注册
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorUtils.createEPHEMERALNode(zkClient, servicePath);
    }
}
