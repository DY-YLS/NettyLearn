package com.yls.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResultProvider;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * HttpObject  客户端和服务端相互通讯的数据
 */
public class TestHttpServerHandler extends SimpleChannelInboundHandler<DecoderResultProvider> {
    //读取客户端数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DecoderResultProvider msg) throws Exception {
        if (msg instanceof HttpRequest) {
            System.out.println("msg 类型：" + msg.getClass());
            HttpRequest httpRequest = (HttpRequest) msg;
            System.out.println("uri:  " + httpRequest.uri());
            //过滤特定资源
            if ("/favicon.ico".equals(httpRequest.uri())) {
                System.out.println("请求是 /favicon.ico ，不做响应");
                return;
            }
            //回复消息给浏览器 （http协议）
            ByteBuf content = Unpooled.copiedBuffer("hello,我是服务器。。", CharsetUtil.UTF_8);

            //构建一个 http response
            DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK, content);
            defaultFullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
            defaultFullHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            //将构建好的 response 返回
            ctx.writeAndFlush(defaultFullHttpResponse);

        }
    }
}
