package com.yls.netty.simple;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;

/**
 * 自定义一个handler,需要继承netty规定好的某个 HandlerAdapter （规范）
 * 这是自定义的handler才能称之为handler
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * 读取客户端发送的数据
     *
     * @param ctx 上下文对象，含有管道 pipeline,通道 channel, 地址
     * @param msg 客户端发送的数据，默认为Object
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

/*        // 可以将耗时长的业务添加到 taskQueue中执行
        ctx.channel().eventLoop().execute(() -> {
            //处理业务
        });*/
        System.out.println("服务器当前线程："+Thread.currentThread().getName());
        //将msg转为 ByteBuf
        //ByteBuf是netty提供，不是NIO中的 ByteBuffer
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("客户端发送消息是：" + buf.toString(CharsetUtil.UTF_8));
        System.out.println("客户端的远程地址：" + ctx.channel().remoteAddress());
    }

    //数据读取完毕
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //将数据写入到缓存并刷新
        //一般要对数据进行编码
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello~,客户端", CharsetUtil.UTF_8));
    }

    //处理异常，一般需要关闭通道
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
