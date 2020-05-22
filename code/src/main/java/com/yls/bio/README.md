
## BIO实现 Socket 通信
**yls**   *2020/5/23*
#### 1.服务端
```java
/**
 * BIO Socket通信实例
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
```

#### 2.客户端
```java
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
                    if (read == -1) break;
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
```
