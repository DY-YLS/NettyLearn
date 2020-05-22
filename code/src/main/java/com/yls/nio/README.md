## NIO 的基本使用
> **yls**   *2020/5/23*
#### NIO中buffer的使用
```java
/**
 * 测试 nio 中 buffer的使用
 * buffer 内部就是一个数组 ： final int[] hb;  、
 *
 * buffer 定义了 position,capacity,limit,mark四个属性来标记buffer中的数据信息
 * 可以通过debug的方式跟踪查看值的变化
 *     private int mark = -1;    标记
 *     private int position = 0;  下一个要被读或写的元素的索引，每次读写都会改变其值，为下次读写做准备
 *     private int limit;   表示缓冲区的当前终点，不能对超过limit限制的缓冲区别进行读写，limit可以修改
 *     private int capacity;   容量，创建buffer时设置，不能被改变
 *
 * buffer  读写切换时需要执行 flip（）方法
 *      public final Buffer flip() {
 *         limit = position;
 *         position = 0;
 *         mark = -1;
 *         return this;
 *     }
 * buffer清除数据时调用 clear（）方法，只改变标记的指向位置，不真正删除底层数组的值
 *     public final Buffer clear() {
 *         position = 0;
 *         limit = capacity;
 *         mark = -1;
 *         return this;
 *     }
 */
public class BasicBuffer {
    public static void main(String[] args) {
        final IntBuffer intBuffer = IntBuffer.allocate(5);
      for (int i = 0; i <intBuffer.capacity() ; i++) {
            intBuffer.put(i*2);
        }
        intBuffer.flip();
        for (int i = 0; i < intBuffer.capacity(); i++) {
            System.out.println(intBuffer.get());
        }
    }
}
```

#### NIO中Socket通信实例
###### 1.服务端
```java
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
```

###### 2.客户端
```java
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

```

#### NIO中Scattering、Gathering的使用
###### 1.服务端
```java
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
```
###### 2.客户端使用上边 Socket通信实例 中的就可以（改一下端口号）

#### NIO 使用 Socket通信实现一个群发系统
###### 1.服务端
```java
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
```

###### 2.客户端
```java
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
```