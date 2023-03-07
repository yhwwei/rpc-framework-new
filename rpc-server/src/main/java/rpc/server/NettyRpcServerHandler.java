package rpc.server;


import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import rpc.codec.kyro.KryoSerializer;
import rpc.enums.RpcResponseCodeEnum;
import rpc.protocol.MessageProtocol;
import rpc.protocol.RpcRequest;
import rpc.protocol.RpcResponse;
import rpc.provider.ServiceProvider;

@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    //专门负责对RpcRequest处理，调用，生成RpcResponse
    private RpcRequestHandler rpcRequestHandler;
    private KryoSerializer kryoSerializer = new KryoSerializer();

    public NettyRpcServerHandler(ServiceProvider serviceProvider){
        rpcRequestHandler = new RpcRequestHandler(serviceProvider);
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        try {
            if (msg instanceof MessageProtocol) {
                MessageProtocol messageProtocol = (MessageProtocol) msg;
                if (messageProtocol.getLen()==0){
                    log.info("长度为0的信息表示 是心跳");
                    //收到心跳，也回复一个
                    MessageProtocol msg2 = new MessageProtocol();
                    msg2.setLen(0);
                    msg2.setContent(new byte[0]);
                    channelHandlerContext.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                    log.info("服务器的心跳返回");
                    return;
                }
                RpcRequest rpcRequest = kryoSerializer.deserialize(messageProtocol.getContent(), RpcRequest.class);

                //接收到请求后要进行处理
                RpcResponse rpcResponse = null;
                MessageProtocol messageProtocol1 = new MessageProtocol();
                rpcResponse = rpcRequestHandler.handlerRequest(rpcRequest);
                if (!(channelHandlerContext.channel().isActive() && channelHandlerContext.channel().isWritable())){
                    rpcResponse.setCode(RpcResponseCodeEnum.FAILURE.getCode());
                    rpcResponse.setMessage(RpcResponseCodeEnum.FAILURE.getMessage());
                    log.error("not writable now, message dropped");
                }
                //将响应用kryo序列化回去
                byte[] bytes = kryoSerializer.serialize(rpcResponse);
                messageProtocol1.setLen(bytes.length);
                messageProtocol1.setContent(bytes);
                //在Netty中所有的IO操作都是异步的，不能立刻得到IO操作的执行结果。如果我们要获取IO操作的结果，就需要注册一个监听器来监听其执行结果。
                channelHandlerContext.writeAndFlush(messageProtocol1).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }finally {
            ReferenceCountUtil.release(msg);
        }

    }

    //心跳协议
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("没有收到心跳，关闭连接");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }


}
