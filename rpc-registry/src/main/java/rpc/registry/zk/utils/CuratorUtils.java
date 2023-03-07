package rpc.registry.zk.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import rpc.enums.RpcConfigEnum;
import rpc.loadbalance.ConsistentHashingWithVirtualNode;
import rpc.utils.PropertiesFileUtil;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
public final class CuratorUtils {

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;
    //定义在zookeeper哪个节点下面
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";

    //记录服务以及对应的服务器ip地址  本地缓存
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    private static CuratorFramework zkClient;

    //zookeeper的ip地址
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    //虚拟节点集合
    private static SortedMap<Integer, String> nodes = new TreeMap<>();

    //单例模式
    private CuratorUtils() {
    }

    //初始化CuratorFramework
    public static CuratorFramework getZkClient() {
        // 如果已经初始化了则直接返回

        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        //RpcConfigEnum 用来记录我们要在哪个配置文件读取信息，读那些信息

        //RpcConfigEnum.RPC_CONFIG_PATH指定配置文件在哪个位置
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        //读取zookeeper服务器的ip+port    RpcConfigEnum.ZK_ADDRESS配置文件里面的配置项，没有则采用默认配置
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;

        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                // the server to connect to (can be a server list)
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        try {
            // wait 30s until connect to the zookeeper
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    //返回虚拟节点给一致性hash算法用
    public static SortedMap<Integer, String> getNodes() {
        return nodes;
    }
    //createPersistentNode和clearRegistry是提供给服务提供者使用的

    /**
     * 服务注册，将消息记录到zookeeper，同时自己本地也缓存
     *
     * @param path 节点路径    eg: /my-rpc/全类名/127.0.0.1:9999
     */
    public static void createEPHEMERALNode(CuratorFramework zkClient, String path) {
        try {
            //如果已经记录这个服务器地址，就跳过
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                //持久化
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 清空一个服务提供者的之前注册在服务中心的信息
     *
     * @param zkClient
     * @param inetSocketAddress 服务提供者的ip
     */

    //其实还可以写一个只删除这个服务提供者的部分服务

    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {

            //某个路径以这个ip结尾则在zookeeper删除掉
            //同时要清空掉自己的缓存
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                    //清空本地缓存
                    SERVICE_ADDRESS_MAP.remove(p);
                    REGISTERED_PATH_SET.remove(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", inetSocketAddress.toString());
    }


    //服务消费者使用的

    /**
     * 获取 服务provider的地址
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
     * @return All child nodes under the specified node
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }

        //如果本地没有记录  则到zookeeper服务器上面去取
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            //放入本地缓存
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            nodes = ConsistentHashingWithVirtualNode.generateNodes(result);
            //第一次从zookeeper获取到信息，同时开启监听，发生变化就进行更新
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * 监听器，在zookeeper下某个节点发生变化时，就更新
     *
     * @param rpcServiceName 全类名   没带上ip地址，标记是什么服务
     */
    //如何使用，消费者要消费某个服务name，从服务注册中心拿到数据后缓存在本地，就对这个服务开启监听器，如果服务提供者的ip地址发生变化，就立即更新到本地缓存
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);

        //监听到节点内容变化后，更新新的服务提供者到本地缓存中
        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            //更新本地缓存
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
            //同时更新虚拟化节点
            nodes = ConsistentHashingWithVirtualNode.generateNodes(serviceAddresses);
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

}
