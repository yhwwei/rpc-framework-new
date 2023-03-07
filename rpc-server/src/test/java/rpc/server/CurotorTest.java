package rpc.server;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

/**
 * @author yhw
 * @version 1.0
 **/
public class CurotorTest {
    public static void main(String[] args) {
        int time =1000;
        int retry =3;
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(time,retry);
        CuratorFramework build = CuratorFrameworkFactory.builder()
                .connectString("127.0.0.1:2181")
                .retryPolicy(retryPolicy)
                .build();
        build.start();
        try {
            build.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/11111");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
