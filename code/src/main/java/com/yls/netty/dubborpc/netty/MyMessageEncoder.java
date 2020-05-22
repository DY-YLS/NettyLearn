package com.yls.netty.dubborpc.netty;

import com.yls.netty.protocoltcp.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * MyMessageEncoder  MyMessageDecoder解决粘包拆包问题
 */
public class MyMessageEncoder extends MessageToByteEncoder<String> {
    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
        //先发送内容长度
        out.writeInt(msg.getBytes().length);
        //发送具体的内容
        out.writeBytes(msg.getBytes());
    }
}
