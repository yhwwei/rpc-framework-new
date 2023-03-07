package rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import rpc.protocol.MessageProtocol;

import java.util.List;

/**
 * @author yhw
 * @version 1.0
 **/
public class MessageProtocolDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        //先读取这批数据长度是多大
        int length = byteBuf.readInt();
        MessageProtocol messageProtocol = new MessageProtocol();
        messageProtocol.setLen(length);
        byte[] content = new byte[length];
        byteBuf.readBytes(content);
        messageProtocol.setContent(content);
        //传送给下一个handler
        list.add(messageProtocol);
    }
}
