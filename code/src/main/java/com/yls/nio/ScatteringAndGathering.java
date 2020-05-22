package com.yls.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * scattering: 分散，把数据写入buffer时，可以采用buffer数组，依次写
 * gathering:  聚合，从buffer中读取数据时，可以采用buffer数组，依次读
 */
public class ScatteringAndGathering {
    public static void main(String[] args) throws IOException {

        //绑定端口到socket,并启动
        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(7000));
        //创建buffer数组
        final ByteBuffer[] byteBuffers = new ByteBuffer[2];
        byteBuffers[0] = ByteBuffer.allocate(4);
        byteBuffers[1] = ByteBuffer.allocate(4);
        //等待客户端连接，连接成功后生成SocketChannel
        final SocketChannel socketChannel = serverSocketChannel.accept();
        //循环读取
        while (true) {
            //从channel读取数据到buffer数组
            long read = socketChannel.read(byteBuffers);

            System.out.println("read=========" + read);
            if (read == 0 || read == -1) {
                break;
            }
            Arrays.asList(byteBuffers).forEach(buffer -> {
                System.out.println("position=" + buffer.position() + ", limit=" + buffer.limit());
            });

            //读写切换
            Arrays.asList(byteBuffers).forEach(buffer -> {
                buffer.flip();
            });

            //将buffer数组中的数据写入channel,显示到客户端
            final long write = socketChannel.write(byteBuffers);

            //将每个buffer置于初始状态
            Arrays.asList(byteBuffers).forEach(buffer -> {
                buffer.clear();
            });
        }


    }
}
