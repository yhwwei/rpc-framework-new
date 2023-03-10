package rpc.test.server;

import rpc.provider.impl.ZkServiceProviderImpl;
import rpc.server.NettyRpcServer;
import rpc.test.service.Hello;
import rpc.test.service.HelloService;

import java.net.UnknownHostException;

/**
 * @author yhw
 * @version 1.0
 **/
public class ServerMain {
    public static void main(String[] args) throws UnknownHostException {
        ZkServiceProviderImpl zkServiceProvider = new ZkServiceProviderImpl("127.0.0.1", 9999);
        zkServiceProvider.addService(Hello.class.getName(),new HelloService());
        NettyRpcServer nettyRpcServer = new NettyRpcServer(zkServiceProvider);
        nettyRpcServer.start();
        while (true){

        }
    }
}
