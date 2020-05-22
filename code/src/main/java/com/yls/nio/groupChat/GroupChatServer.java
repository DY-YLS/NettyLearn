package com.yls.nio.groupChat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * 简单的群聊系统（NIO 实现）
 * 服务端：可以监听客户端的上线和离线，可以接收客户端发送的数据并转发到其它客户端
 * 客户端：可以不阻塞的发送数据和接收其它客户端发送的数据
 */
public class GroupChatServer {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private static final int port = 7999;

    public GroupChatServer() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    //监听
    public void listen() {
        try {
            while (true) {
                final int select = selector.select(2000);
                if (select > 0) {//有事件发生
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        //获取key
                        SelectionKey key = iterator.next();
                        //根据key对应的通道发生的事件做出处理
                        if (key.isAcceptable()) {//如果是 isAcceptable,有新的连接
                            //这里的key对应的channel一定是serverSocketChannel
                            //为该客户端生成一个socketChannel
//                            SocketChannel socketChannel = serverSocketChannel.accept();
                            SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                            //将新生成的socketChannel设置为非阻塞模式，否则会抛出异常
                            socketChannel.configureBlocking(false);
                            //将socketChannel注册到selector,关心事件为OP_READ，并关联一个buffer
                            socketChannel.register(selector, SelectionKey.OP_READ);
                            //提示
                            System.out.println(socketChannel.getRemoteAddress() + ",上线了");
                        }
                        if (key.isReadable()) { //发生 isReadable 事件，表示有新数据发送过来
                            //专门写方法，处理读操作
                            readData(key);
                        }

                        //手动从集合中移除当前的key，防止重复操作
                        iterator.remove();
                    }
                } else {
                    System.out.println("等待。。。");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readData(SelectionKey key) {

        SocketChannel channel = null;
        try {
            //根据key反向获取到相应的channel
            channel = (SocketChannel) key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(128);
            //将channel中的数据读到buffer中
            final int read = channel.read(byteBuffer);
            String msg = new String(byteBuffer.array(), 0, read);
            System.out.println("from 客户端： " + msg);
            //转发消息到其它客户端（除了自己），专门写一个方法
            sendMsgToOthers(msg, channel);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                System.out.println(channel.getRemoteAddress() + "离线了。。");
                //关闭通道
                channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    private void sendMsgToOthers(String msg, SocketChannel self) {

        final Set<SelectionKey> keys = selector.keys();
        keys.forEach((key) -> {
             SelectableChannel channel = key.channel();
            if (channel instanceof SocketChannel && channel != self) {
                SocketChannel socketChannel=(SocketChannel)channel;
                final ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                try {
                    socketChannel.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws IOException {
        final GroupChatServer groupChatServer = new GroupChatServer();
        groupChatServer.listen();
    }
}

