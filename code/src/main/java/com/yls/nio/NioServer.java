package com.yls.nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * 同步 非阻塞
 * FileChannel.transferTo实现了零拷贝，效率高，三十在windows中一次只能传8M,所以大文件需要断点续传
 */
public class NioServer {
    public static void main(String[] args) throws IOException {
        //创建一个ServerSocketChannel
        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //绑定一个端口，在服务端监听
        serverSocketChannel.socket().bind(new InetSocketAddress(7777));
        //设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //得到一个selector对象
        Selector selector = Selector.open();
        //serverSocketChannel注册到selector,关心 事件 OP_ACCEPT
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        //循环等待客户端连接
        while (true) {
            //等待一秒，若没有事件发生，返回
            if (selector.select(1000) == 0) {
                System.out.println("服务器等待了1秒，没有请求连接。。。。");
                continue;
            }
            //若返回的值>0，说明已经获取到相关的事件，则获取到相关的selectionKeys集合
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            //使用迭代器遍历
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                //获取到相应的key
                SelectionKey key = iterator.next();
                //根据key对应的通道发生的事件做出处理
                if (key.isAcceptable()) {//如果是 isAcceptable,有新的连接
                    //这里的key对应的channel一定是serverSocketChannel
                    //为该客户端生成一个socketChannel
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    //将新生成的socketChannel设置为非阻塞模式，否则会抛出异常
                    socketChannel.configureBlocking(false);
                    //将socketChannel注册到selector,关心事件为OP_READ，并关联一个buffer
                    socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(128));
                }
                if (key.isReadable()) { //发生 isReadable 事件，表示有新数据发送过来
                    //根据key反向获取到相应的channel
                    SocketChannel channel = (SocketChannel) key.channel();
                    //获取到channel关联的buffer
                    ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
                    //先将buffer置于初始状态
                    byteBuffer.clear();
                    //将channel中的数据读到buffer中
                    channel.read(byteBuffer);
                    //buffer读写切换
                    byteBuffer.flip();
                    //从buffer中读有效数据到bytes
                    //byteBuffer.array()直接返回buffer底层数组，如果后面发送的数据比之前发送的少，会将之前获取的值获取出来
                    byte[] bytes = new byte[byteBuffer.limit()];
                    byteBuffer.get(bytes);
                    System.out.println("from 客户端： " + new String(bytes));
                }

                //手动从集合中移除当前的key，防止重复操作
                iterator.remove();
            }
        }
    }
}
