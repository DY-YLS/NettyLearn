package com.yls.netty.dubborpc.provider;

import com.yls.netty.dubborpc.netty.NettyServer;
import com.yls.netty.dubborpc.netty.NettyServerHandler;


public class ServerBootStrap {
    public static void main(String[] args) {
        NettyServerHandler.setClassNameMapping(new HelloServiceImpl());
        NettyServer.start(9999);
    }
}
