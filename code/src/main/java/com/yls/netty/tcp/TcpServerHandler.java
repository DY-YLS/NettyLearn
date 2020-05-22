package com.yls.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import jdk.nashorn.internal.ir.CallNode;

import java.nio.charset.Charset;

public class TcpServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private int count;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        final String s = msg.toString(Charset.forName("utf-8"));
       /* byte[] bytes=new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        final String s = new String(bytes, CharsetUtil.UTF_8);*/

        System.out.println(s);
        System.out.println("次数："+(++count));
    }
}
