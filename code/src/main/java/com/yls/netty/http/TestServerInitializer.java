package com.yls.netty.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class TestServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //向管道加入处理器
        //获取管道
        ch.pipeline()
                .addLast("myHttpServerCodec",new HttpServerCodec())//添加一个netty提供的处理http的编码-解码器
                .addLast(new TestHttpServerHandler());//添加一个自定义的handler
    }
}
