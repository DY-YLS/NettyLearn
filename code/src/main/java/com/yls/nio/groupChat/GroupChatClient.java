package com.yls.nio.groupChat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class GroupChatClient {
    private String ip = "127.0.0.1";
    private int port = 7999;
    private Selector selector;
    private SocketChannel socketChannel;
    private String name;

    public GroupChatClient() throws IOException {
        socketChannel = SocketChannel.open(new InetSocketAddress(ip, port));
        socketChannel.configureBlocking(false);
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);
        name = socketChannel.getLocalAddress().toString().substring(1);
        System.out.println(name + " is start ..");
    }

    //发送消息
    public void sendData(String msg) throws IOException {
        msg = name + " 说：" + msg;
        socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
    }

    //接收消息
    public void rec() {
        try {
            while (true) {
                final int select = selector.select();
                if (select > 0) {
                    final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey next = iterator.next();
                        if (next.isReadable()) {
                            SocketChannel channel = (SocketChannel) next.channel();
                            final ByteBuffer allocate = ByteBuffer.allocate(128);
                            final int read = channel.read(allocate);
                            System.out.println(new String(allocate.array(), 0, read));
                        }
                        iterator.remove();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        final GroupChatClient groupChatClient = new GroupChatClient();
        //一个线程专门接收数据
        new Thread(() -> {
            groupChatClient.rec();
        }, "接收数据线程").start();
        //主线程用来发送数据
        final Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            final String s = scanner.nextLine();
            groupChatClient.sendData(s);
        }
    }
}
