package com.yls.netty.protocoltcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class MyMessageDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //先读取要接收的字节长度
        final int len = in.readInt();
        final byte[] bytes = new byte[len];
        //再根据长度读取真正的字节数组
        in.readBytes(bytes);
        final Message message = new Message();
        message.setLen(bytes.length);
        message.setContent(bytes);
        out.add(message);
    }
}
