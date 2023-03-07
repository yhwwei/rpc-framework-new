package rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import rpc.codec.MessageProtocolDecoder;
import rpc.codec.MessageProtocolEncoder;
import rpc.codec.kyro.KryoSerializer;
import rpc.loadbalance.LoadBalance;
import rpc.netty.transport.RpcRequestTransport;
import rpc.protocol.MessageProtocol;
import rpc.protocol.RpcRequest;
import rpc.protocol.RpcResponse;
import rpc.registry.ServiceDiscovery;
import rpc.registry.zk.ZkServiceDiscoveryImpl;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author yhw
 * @version 1.0
 **/
@Slf4j
public class NettyRpcClient {
    private final UnprocessedRequests unprocessedRequests;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final RpcRequestTransport rpcRequestTransport;
    private final EventLoopGroup eventLoopGroup;
    private KryoSerializer kryoSerializer = new KryoSerializer();
    public NettyRpcClient(RpcRequestTransport rpcRequestTransport){
        this.channelProvider = new ChannelProvider();
        eventLoopGroup = new NioEventLoopGroup();
        this.rpcRequestTransport = rpcRequestTransport;
        unprocessedRequests = new UnprocessedRequests();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline p = socketChannel.pipeline();
                        p.addLast(new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS));
                        p.addLast(new MessageProtocolDecoder());
                        p.addLast(new MessageProtocolEncoder());
                        p.addLast(new NettyRpcClientHandler(unprocessedRequests));
                    }
                });
    }
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();

        //异步监听
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // build return value
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // get server address
        String ipPort = rpcRequestTransport.sendRpcRequest(rpcRequest);

        //字符串的 ip:port
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));
        // get  server address related channel
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            // 发送消息后，先放入未处理队列中
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            //发送rpcRequest之前，要变为MessageProtocol  解决TCP粘包拆包
            MessageProtocol rpcMessage = new MessageProtocol();
            byte[] content = kryoSerializer.serialize(rpcRequest);
            rpcMessage.setLen(content.length);
            rpcMessage.setContent(content);

            //发送成功后，使用CompletableFuture，异步获取结果，不用阻塞
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }

        return resultFuture;
    }

    //获取跟其他服务器连接的channel
    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }


}
