package com.yls.netty.heartbeat;

import com.yls.netty.simple.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * netty心跳检测机制
 * 可以防止 客户端异常终止，服务间没有检测到的情况
 */
public class Server {
    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //创建服务端的启动对象，并使用链式编程来设置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup) //设置两个线程组
                    .channel(NioServerSocketChannel.class)//使用NioServerSocketChannel 作为服务器的通道实现
                    .option(ChannelOption.SO_BACKLOG, 128)//设置线程队列的连接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //设置一直保持活动连接状态
                    .handler(new LoggingHandler(LogLevel.INFO)) //给bossGroup 添加一个日志处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {//设置一个通道测试对象
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //给pipeline设置通道处理器
                            final ChannelPipeline pipeline = ch.pipeline();
                            /**
                             * IdleStateHandler 是netty提供的处理空闲状态的处理器
                             *
                             * long readerIdleTime ： 表示多长时间没读，就会发送一个心跳检测包检测是否连接
                             * long writerIdleTime：  表示多长时间没写，就会发送一个心跳检测包检测是否连接
                             * long allIdleTime：    表示多长时间没读写，就会发送一个心跳检测包检测是否连接
                             *
                             * 当IdleStateHandler触发后，将会传递给管道的下一个handler处理
                             * 通过调用下一个handler的userEventTriggered()方法，在该方法中去处理空闲
                             */
                            pipeline.addLast(new IdleStateHandler(3,5,7, TimeUnit.SECONDS));

                            //加入一个对空闲检测进一步处理的处理器（自定义）
                            pipeline.addLast(new ServerHandler());

                        }
                    });//给 workerGroup 的EventLoop对应的管道设置处理器
            //启动服务器，并绑定端口并且同步
            ChannelFuture channelFuture = serverBootstrap.bind(7000).sync();

            //给 channelFuture 注册监听器，监听关心的事件
//            channelFuture.addListener((future) -> {
//                if (future.isSuccess()) {
//                    System.out.println("监听端口成功。。。");
//                } else {
//                    System.out.println("监听端口失败。。。");
//                }
//            });
            //对关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
