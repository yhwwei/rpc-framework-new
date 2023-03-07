package rpc.loadbalance;

import rpc.protocol.RpcRequest;

import java.util.LinkedList;
import java.util.List;

/**
 * @author yhw
 * @version 1.0
 **/
public class PollingBalance implements LoadBalance{
    //轮询算法
    int idx=0;

    @Override
    public String getServerBalance(String ip, List<String> nodes) {

        String s = nodes.get(idx);
        idx=(idx+1)%nodes.size();
        return s;
    }
}
