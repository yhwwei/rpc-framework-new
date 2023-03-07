package rpc.server;

import org.apache.curator.framework.CuratorFramework;
import rpc.protocol.RpcRequest;
import rpc.protocol.RpcResponse;
import rpc.provider.ServiceProvider;
import rpc.provider.impl.ZkServiceProviderImpl;
import rpc.registry.zk.utils.CuratorUtils;
import rpc.service.Hello;
import rpc.service.HelloService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yhw
 * @version 1.0
 **/
class RpcRequestHandlerTest {
    public static void main(String[] args) {
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        ZkServiceProviderImpl zkServiceProvider = new ZkServiceProviderImpl("127.0.0.1",9999);
        System.out.println(Hello.class.getName());
        zkServiceProvider.addService(Hello.class.getName(),new HelloService());
        RpcRequestHandler rpcRequestHandler = new RpcRequestHandler(zkServiceProvider);
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId("1111");
        rpcRequest.setInterfaceName(Hello.class.getName());
        rpcRequest.setParameters(new Object[]{"111"});
        rpcRequest.setParamTypes(new Class[]{String.class});
        rpcRequest.setMethodName("hello");
        System.out.println(rpcRequest);
        RpcResponse rpcResponse = rpcRequestHandler.handlerRequest(rpcRequest);
        System.out.println(CuratorUtils.getChildrenNodes(zkClient,"rpc.service.Hello"));
        System.out.println(rpcResponse);
    }
}