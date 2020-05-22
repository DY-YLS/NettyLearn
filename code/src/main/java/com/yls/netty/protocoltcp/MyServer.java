package com.yls.netty.protocoltcp;

import com.yls.netty.simple.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 自定义协议解决tcp粘包拆包问题
 * 发送端：先传要传的字节长度，再传具体的字节数组
 * 接收端：先接收字节长度，再根据字节长度读取字节
 */
public class MyServer {
    public static void main(String[] args) throws InterruptedException {
        //创建两个线程组 bossGroup workerGroup，两个都是无限循环
        //bossGroup 只是处理连接请求
        //workerGroup  真正处理客户端的业务
        // 若不传参数，bossGroup workerGroup中默认的子线程数（NioEventLoop） = 电脑处理器数 * 2
        // Runtime.getRuntime().availableProcessors() 可以获取到处理器数
        // bossGroup只处理请求的连接，我们这里设置线程数为1
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //创建服务端的启动对象，并使用链式编程来设置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup) //设置两个线程组
                    .channel(NioServerSocketChannel.class)//使用NioServerSocketChannel 作为服务器的通道实现
                    .option(ChannelOption.SO_BACKLOG, 128)//设置线程队列的连接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //设置一直保持活动连接状态
                    .childHandler(new ChannelInitializer<SocketChannel>() {//设置一个通道测试对象
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //给pipeline设置通道处理器
                            ch.pipeline()
                                    .addLast(new MyMessageDecoder())
                                    .addLast(new MyServerHandler());
                        }
                    });//给 workerGroup 的EventLoop对应的管道设置处理器
            //启动服务器，并绑定端口并且同步
            ChannelFuture channelFuture = serverBootstrap.bind(6999).sync();

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
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
