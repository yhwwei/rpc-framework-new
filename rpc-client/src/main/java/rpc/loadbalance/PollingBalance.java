package rpc.loadbalance;

import java.util.LinkedList;
import java.util.List;

/**
 * @author yhw
 * @version 1.0
 **/
public class PollingBalance {
    //轮询算法
    int idx=0;
    private List<String> realGroups = new LinkedList<>();
    public PollingBalance(List<String> nodes){
        this.realGroups = nodes;
    }
    public String getServer(){
        return realGroups.get(idx++%realGroups.size());
    }
}
