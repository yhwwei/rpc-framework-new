# 用netty+kryo+zookeeper实现rpc框架
上一次写了一个简易的RPC框架，provider和consumer之间使用的是BIO通信，性能差，序列化采用了JSON虽然可读性好，但是效率也差了一些。
所以这次改进使用NIO通信，采取Kyro进行双方序列化通信。服务注册中心改用zookeeper。

# 注意注意 避坑
在使用curatorframework一定要看好zookeeper的版本的，不然会报错

# 模块划分
①rpc-client   服务消费者，在这里使用动态代理，在invocationHandler的实现类的invoke方法中实现远程调用的细节
②rpc-common   一些工具类以及一些使用东西
③rpc-protocol  定义网络传输协议，如response、request、采用什么序列化，encode、decode策略；因为采用netty实现网络连接， 我们要解决TCP粘包拆包问题，在request、response序列化后，要变为MessageProtocol用netty传输。
④rpc-registry   服务注册中心的实现，采用zookeeper，在里面定义zookeeper里面数据CRUD的操作
⑤rpc-server  服务提供者

# 大概流程
服务提供者往服务注册中心注册服务后，本地要维护服务对应的单例对象，在解析RpcRequest后，找到对应的单例对象实现方法，然后将结果封装成RpcResponse，再封装成MessageProtocol /
，然后经过netty的一系列Handler处理，如将MessageProtocol进行encode以及decode发送给服务消费者。

提供者和消费者之间建立连接后，会采用心跳机制避免断开连接，消费者30s发送一次心跳，如果提供者60s内没收到心跳，则断开连接。

提供者的流程：从zookeeper拉取数据缓存在本地，同时建立一个监听器，但zookeeper上面的数据变化时，能够更新到本地，采用轮询或者hash一致性负载均衡算法，将请求发送给提供者。
