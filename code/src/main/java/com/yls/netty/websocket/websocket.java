package com.yls.netty.websocket;

import com.yls.netty.heartbeat.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * 服务端
 * netty 实现 websocket 全双工通信
 */
public class websocket {
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
                            //因为基于http协议，加入http的编码和解码器
                            pipeline.addLast(new HttpServerCodec());
                            //是以块方式写，添加ChunkedWriteHandler处理器
                            pipeline.addLast(new ChunkedWriteHandler());
                            //http 数据在传输过程中是分段，当浏览器发送大量数据时，就会发送多次http请求
                            //HttpObjectAggregator可以将多个段聚合
                            pipeline.addLast(new HttpObjectAggregator(8192));
                            /**
                             *websocket 数据是以帧（frame）的形式传递
                             * 可以看到WebSocketFrame 下面有六个子类
                             * WebSocketServerProtocolHandler 的核心功能是将http协议升级为ws协议，保持长连接
                             */
                            pipeline.addLast(new WebSocketServerProtocolHandler("/hello"));
                            //自定义handler,处理业务逻辑
                            pipeline.addLast(new MyTextWebSocketFrameHandler());

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
