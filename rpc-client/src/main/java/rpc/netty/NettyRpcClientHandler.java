package rpc.netty;

import io.netty.channel.*;

import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import rpc.codec.kyro.KryoSerializer;
import rpc.protocol.MessageProtocol;
import rpc.protocol.RpcResponse;



/**
 * @author yhw
 * @version 1.0
 **/
@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter{
    private final UnprocessedRequests unprocessedRequests;
    private KryoSerializer kryoSerializer = new KryoSerializer();
    public NettyRpcClientHandler(UnprocessedRequests unprocessedRequests){
        this.unprocessedRequests = unprocessedRequests;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            log.info("client receive msg: [{}]", msg);
            if (msg instanceof MessageProtocol) {
                MessageProtocol tmp = (MessageProtocol) msg;
                if (tmp.getLen()==0){
                    return;
                }
                byte[] content = tmp.getContent();
                RpcResponse rpcResponse = kryoSerializer.deserialize(content, RpcResponse.class);

                //收到回复后，在未完成的队列中删除掉该request
                unprocessedRequests.complete(rpcResponse);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }


    //发送心跳
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                log.info("write idle happen [{}]", ctx.channel().remoteAddress());
                MessageProtocol messageProtocol = new MessageProtocol();
                messageProtocol.setLen(0);
                messageProtocol.setContent(new byte[0]);
                ctx.channel().writeAndFlush(messageProtocol).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
