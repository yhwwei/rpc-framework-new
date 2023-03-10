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

# 开发过程
①rpc-registry
loadbalance包下面定义了 两种负载均衡算法
registry包下面定义了服务发现和服务注册
registry.zk.utils  包下面的CuratorUtils帮助我们连接zookeeper，实现对zookeeper数据的CRUD
同时会把zookeeper上面的数据缓存在一个ConcurrentHashMap里面，这样子不用每次都到zookeeper里面取，同时在服务发现的时候，开启对zookeeper那个服务节点的监听，当有数据更新的时候就会同步到本地
服务发现时嵌入了负载均衡，返回负载均衡后的服务提供者的ip:port
②rpc-server
provider包 会调用rpc-registry的服务注册，一方面将服务注册到服务中心，一方面本地要维护一个自己对这个服务的单例实例对象
RpcRequestHandler类专门对rpcRequest处理，生成rpcResponse
NettyRpcServerHandler 心跳机制、将请求交给RpcRequestHandler处理
# 可优化部分
代码中有些硬编码，后续可以进行优化
采取注解的方式替代配置文件
③rpc-client
proxy包 采用JDK自带的动态代理  内部invoke进行远程调用
netty包下面的ChannelProvider 维护跟服务器连接的channel
当NettyRpcClient发送请求后，这个请求会被记录在UnprocessedRequests 中的一个未处理Map，之后如果收到服务器的响应则删除未处理
采用CompletableFuture异步获取响应结果

④采用MessageProtocol解决TCP 粘包拆包问题，也就是为什么RpcRequest和RpcResponse要先转化为MessageProtocol的原因
RpcRequest和RpcResponse要先采用Kryo序列化为二进制数组
Kryo是线程不安全的，要用ThreadLocal
在rpc-protocolmodule 里面定义MessageProtocolDecoder解码、MessageProtocolEncoder编码器，在使用时要在pipeline里面添加

# 测试
在rpc-simple
启动zookeeper
然后启动ServerMain，ClientMain，看控制台输出打印成功