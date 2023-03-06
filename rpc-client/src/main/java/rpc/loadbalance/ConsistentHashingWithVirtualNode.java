package rpc.loadbalance;

import rpc.utils.HashUtil;

import java.util.*;

/**
 * @author yhw
 * @version 1.0
 * @description   使用一致性hash算法+虚拟节点实现负载均衡
 **/
public class ConsistentHashingWithVirtualNode {
    //真实集群列表
    private List<String> realGroups = new LinkedList<>();

    //虚拟节点映射
    private  SortedMap<Integer, String> virtualNodes = new TreeMap<>();

    private static final int VIRTUAL_NODE_NUM = 1000;

    public ConsistentHashingWithVirtualNode(List<String> nodes){
        this.realGroups = nodes;

        //添加虚拟节点
        for (String realGroup: realGroups) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                String virtualNodeName = getVirtualNodeName(realGroup, i);
                int hash = HashUtil.getHash(virtualNodeName);
//                System.out.println("[" + virtualNodeName + "] launched @ " + hash);
                virtualNodes.put(hash, virtualNodeName);
            }
        }
    }
    //生成虚拟节点
    private  String getVirtualNodeName(String realName, int num) {
        return realName + "&&VN" + String.valueOf(num);
    }

    //返回虚拟节点对于的真实节点（传入的参数也可以是真实ip地址会返回本身）
    private String getRealNodeName(String virtualName) {
        return virtualName.split("&&")[0];
    }

    /**
     * 获取客户端ip通过一致性hash算法后对应到哪个服务器
     * @param widgetKey
     * @return
     */
    private  String getServer(String widgetKey) {
        int hash = HashUtil.getHash(widgetKey);
        // 只取出所有大于该hash值的部分而不必遍历整个Tree
        SortedMap<Integer, String> subMap = virtualNodes.tailMap(hash);
        String virtualNodeName;
        if (subMap == null || subMap.isEmpty()) {
            // hash值在最尾部，应该映射到第一个group上
            virtualNodeName = virtualNodes.get(virtualNodes.firstKey());
        }else {
            virtualNodeName = subMap.get(subMap.firstKey());
        }
        return getRealNodeName(virtualNodeName);
    }


}
