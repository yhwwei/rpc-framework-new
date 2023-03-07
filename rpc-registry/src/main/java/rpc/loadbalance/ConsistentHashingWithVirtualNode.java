package rpc.loadbalance;

import rpc.registry.zk.utils.CuratorUtils;
import rpc.utils.HashUtil;

import java.util.*;

/**
 * @author yhw
 * @version 1.0
 * @description   使用一致性hash算法+虚拟节点实现负载均衡
 **/
public class ConsistentHashingWithVirtualNode implements LoadBalance{
    //每个ip对应的虚拟节点数量
    private static final int VIRTUAL_NODE_NUM = 100;
    public static SortedMap<Integer, String> generateNodes(List<String> realNodes){
        SortedMap<Integer, String> nodes = new TreeMap<>();
        //添加虚拟节点
        for (String node: realNodes) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                String virtualNodeName = getVirtualNodeName(node, i);
                int hash = HashUtil.getHash(virtualNodeName);
//                System.out.println("[" + virtualNodeName + "] launched @ " + hash);
                //
                nodes.put(hash, virtualNodeName);
            }
        }
        return nodes;
    }

    //生成虚拟节点名字
    private static String getVirtualNodeName(String realName, int num) {
        return realName + "&&VN" + String.valueOf(num);
    }

    //返回虚拟节点对于的真实节点（传入的参数也可以是真实ip地址会返回本身）
    private static String getRealNodeName(String virtualName) {
        return virtualName.split("&&")[0];
    }

    /**
     * 获取客户端ip通过一致性hash算法后对应到哪个服务器
     * @param ip
     * @return
     */
    @Override
    public String getServerBalance(String ip, List<String> nodes) {
        int hash = HashUtil.getHash(ip);

        //虚拟节点保存在CuratorUtils中
        SortedMap<Integer, String> virtualNodes = CuratorUtils.getNodes();

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
