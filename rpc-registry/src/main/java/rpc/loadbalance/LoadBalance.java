package rpc.loadbalance;

import rpc.protocol.RpcRequest;

import java.util.List;

/**
 * @author yhw
 * @version 1.0
 **/
public interface LoadBalance {
    String getServerBalance(String ip,List<String> nodes);
}
