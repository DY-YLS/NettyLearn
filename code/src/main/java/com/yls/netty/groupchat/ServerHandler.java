package com.yls.netty.groupchat;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerHandler extends SimpleChannelInboundHandler<String> {

    // 定义一个 channel 组，管理所有的channel
    //GlobalEventExecutor.INSTANCE 全局的事件执行器，是一个单例
    // 必须用 static 修饰，表示类变量
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 表示连接建立，一旦连接，第一个被执行
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //将该客户端加入聊天通知给其它客户端
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String s = String.format("客户端 %s 加入聊天 %s", ctx.channel().remoteAddress(), dateTime);
        //该方法会自动遍历所有channel，并发送消息，不需要自己遍历
//        channelGroup.writeAndFlush(s);
        channelGroup.writeAndFlush(s);
        //将该客户端的channel 加入channel 组
        channelGroup.add(ctx.channel());
    }

    //断开连接，handlerRemoved 执行后，会自动将当前channel从channelGroup移除
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.channel();
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String s = String.format("客户端 %s 离开了 %s", ctx.channel().remoteAddress(), dateTime);
        //该方法会自动遍历所有channel，并发送消息，不需要自己遍历
        channelGroup.writeAndFlush(s);
    }

    //表示channel处于活动状态
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + " 上线了。。");
    }

    //表示channel处于非活动状态
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "离线了。。");
    }

    //读取数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        final Channel channel = ctx.channel();
        channelGroup.forEach(ch -> {
            if (channel != ch) {
                ch.writeAndFlush("客户端 " + channel.remoteAddress() + " 发送了消息：" + msg);
            } else {
                ch.writeAndFlush("自己发送了消息：" + msg);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
