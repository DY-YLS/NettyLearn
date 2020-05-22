package com.yls.bio;

import sun.nio.ch.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BIO  实例
 */
public class BioServer {
    public static void main(String[] args) throws IOException {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        //创建serverSocket,并绑定端口
        //构造函数内部与 serverSocket.bind(new InetSocketAddress("localhost",6666)); 实现一致
        ServerSocket serverSocket = new ServerSocket(6666);

        while (true) {
            //监听，等待客户端连接
            Socket socket = serverSocket.accept();
            System.out.println("连接到一个客户端");
            System.out.println(socket.getPort());
            System.out.println(socket.getInetAddress());

            //接收到客户端后，就启动一个新线程，不然其它后进来的线程无法连接
            threadPool.execute(() -> {
                try (
                        //获取socket 输入流，用来接收数据，不阻塞
                        InputStream inputStream = socket.getInputStream();
                        ////获取socket 输出流流，用来发送数据，不阻塞
                        OutputStream outputStream = socket.getOutputStream()
                ) {

                    System.out.println("获取socket 输入流成功。。。");
                    while (true) {
                        byte[] bytes = new byte[2];
                        //阻塞
                        int read = inputStream.read(bytes);
                        System.out.println("read======" + read);
                        System.out.println("从输入流获取值成功。。。");
                        System.out.println(new String(bytes, 0, read));
                        //向客户端发送数据
                        outputStream.write("22222......".getBytes());
                        outputStream.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("finally 执行了。。。");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            });

        }
    }
}