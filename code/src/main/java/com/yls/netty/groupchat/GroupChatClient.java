package com.yls.netty.groupchat;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Scanner;

public class GroupChatClient {
    private String ip;
    private int port;

    public GroupChatClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void run() {
        NioEventLoopGroup group = new NioEventLoopGroup(1);

        try {
            final Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new StringDecoder())
                                    .addLast(new StringEncoder())
                                    .addLast(new ClientHandler());
                        }
                    });
            final ChannelFuture sync = bootstrap.connect(ip, port).sync();
            final Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()){
                final String s = scanner.nextLine();
                sync.channel().writeAndFlush(s);
            }
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

    }

    public static void main(String[] args) {
        new GroupChatClient("127.0.0.1",7000).run();
    }
}
