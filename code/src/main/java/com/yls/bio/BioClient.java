package com.yls.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class BioClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (
                //连接服务端
                Socket socket = new Socket("127.0.0.1", 6999);
                //获取socket输出流，用于发数据
                OutputStream outputStream = socket.getOutputStream();
                //获取socket输入流，用于接收数据
                InputStream inputStream = socket.getInputStream()
        ) {

            System.out.println("连接服务端成功。。。。。");
            while (true) {
                //发数据
                outputStream.write(scanner.nextLine().getBytes());
                System.out.println("发送数据成功。。。。");
                byte[] bytes = new byte[128];
                //接收数据
                while (true) {
                    int read = inputStream.read(bytes);
                    if(read==-1) break;
                    System.out.println("接收数据成功");
                    System.out.println(new String(bytes, 0, read));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
    }
}
