package com.yls.nio;

import com.yls.nio.groupChat.GroupChatServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioClient {
    public static void main(String[] args) throws IOException {
        //获取一个 socketChannel
        final SocketChannel socketChannel = SocketChannel.open();
        //设置 socketChannel 非阻塞
        socketChannel.configureBlocking(false);
        //提供服务端的ip和端口，连接服务端，不阻塞
        //通过 socketChannel.finishConnect() 判断是否连接成功
        boolean connect = socketChannel.connect(new InetSocketAddress("127.0.0.1", 6999));
        if (!connect) {
            while (!socketChannel.finishConnect()) {
                System.out.println("因为连接需要时间，客户端不会阻塞，可以做其它事情。。。");
            }
        }
        //连接成功后。。。。
        System.out.println("1...");
        String s = "大忽忽";
        // Wraps a byte array into a buffer
        ByteBuffer byteBuffer = ByteBuffer.wrap(s.getBytes());
        //发送数据
        final int write = socketChannel.write(byteBuffer);
        System.out.println("2....");
        System.in.read();
    }
}
