#netty的基本使用
> **yls**   *2020/5/23*
## 有了NIO,为什么还要有netty?
1. NIO类库和和API繁杂，需要熟练掌握 Selector,ServerSocketChannel,SocketChannel,ByteBuffer等
2. 需要具备其它额外的技能，要熟悉java多线程编程，因为NIO涉及到Reactor模式，也要熟悉网络编程
3. 开发工作和难度都比较大，例如客户端面临 断连重连、网络闪断、半包读写、网络拥塞和异常流的处理等等
4. JDK NIO的Bug:臭名昭著的Epoll Bug,他会导致selector空轮询，最终导致cpu 100%,知道jdk 1.7仍旧存在，
没有被根本解决

## Netty模型
1. netty抽象出两组线程池（NioEventLoopGroup），BossGroup和workerGroup,BossGroup专门接收客户端连接，workGroup专门负责网络读写操作
2. NioEventLoop是BossGroup或workerGroup中不断循环执行处理任务的线程，每个NioEventLoop都有一个selector，
用于监听绑定在其上的socket网络通信
3. NioEventLoop 内部采用串行化设计，从消息的读取->解码->处理->编码->发送，始终又IO线程 NioEventLoop负责

* NioEventLoopGroup下包含多个NioEventLoop
* 每个NioEventLoop中包含一个selector,一个taskQueue
* 每个NioEventLoop的selector可以注册监听多个NioChannel
* 每个NioChannel只会绑定在唯一的NioEventLoop
* 每个NioChannel都绑定有一个自己的ChannelPipeline

## Netty的异步模型
#### Netty的异步模型的基本介绍
1. 异步的概念和同步相对，当一个异步过程调用发生后，调用者不能立刻得到结果，异步调用完成后，通过状态，通知和回调来通知调用者
2. Netty中的I/O操作是异步的，包括 bind,write,connect等操作会简单的返回一个ChannelFuture.
3. Netty的异步模型是建立在future和callback之上的，调用者通过Future-Listener机制获得结果。
#### Future-Listener 机制
1. 当 future对象刚刚创建时，处于未完成状态，调用者可以通过返回的 ChannelFuture 来获取操作执行的状态，
注册监听函数来执行完成后的操作
2. 常见操作如下：

|方法名称|含义|
|----|-----|
|isDone|判断当前操作是否完成，不一定成功，可能是抛出异常，程序中断等|
|isSuccess|判断当前操纵是否成功|
|cause|获取已完成的当前操作失败的原因|
|isCancelled|判断当前操作是否被取消|
|addListener|注册监听器|
3. 举例说明
```
            //启动服务器，并绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(6999);
            //给 channelFuture 注册监听器，监听关心的事件
            channelFuture.addListener((future) -> {
                if (future.isSuccess()) {
                    System.out.println("监听端口成功。。。");
                } else {
                    System.out.println("监听端口失败。。。");
                }
            });
```
4. 总结：相比传统阻塞I/O,线程会阻塞，直到操作完成；异步处理不会造成线程阻塞，线程在I/O操作期间可以执行别的程序，
在高并发情形下会更稳定更高的吞吐量。

## netty的简单使用
#### 1.创建客户端
```java
public class NettyClient {
    public static void main(String[] args) throws InterruptedException {
        //客户端需要一个事件循环组就可以
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建客户端的启动对象 bootstrap ，不是 serverBootStrap
            Bootstrap bootstrap = new Bootstrap();
            //设置相关参数
            bootstrap.group(group) //设置线程组
                    .channel(NioSocketChannel.class) //设置客户端通道的实现数 （反射）
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new NettyClientHandler()); //加入自己的处理器
                        }
                    });

            System.out.println("客户端 ready is ok..");
            //连接服务器
            final ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 6999).sync();
            //对关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
```
#### 2.客户端自定义处理器
```java
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    //当通道就绪就会触发该方法
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.copiedBuffer("hi~,服务端。。。。", CharsetUtil.UTF_8));
    }

    //当通道有读取事件会触发
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("服务器回复的数据： " + buf.toString(CharsetUtil.UTF_8));
        System.out.println("服务器的地址： " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

#### 3.创建服务端
```java
/**
 * netty 简单示例
 * 客户端像服务端发送一个消息，服务端向客户端回复一条消息
 */
public class NettyServer {
    public static void main(String[] args) throws InterruptedException {
        //创建两个线程组 bossGroup workerGroup，两个都是无限循环
        //bossGroup 只是处理连接请求
        //workerGroup  真正处理客户端的业务
        // 若不传参数，bossGroup workerGroup中默认的子线程数（NioEventLoop） = 电脑处理器数 * 2
        // Runtime.getRuntime().availableProcessors() 可以获取到处理器数
        // bossGroup只处理请求的连接，我们这里设置线程数为1
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //创建服务端的启动对象，并使用链式编程来设置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup) //设置两个线程组
                    .channel(NioServerSocketChannel.class)//使用NioServerSocketChannel 作为服务器的通道实现
                    .option(ChannelOption.SO_BACKLOG, 128)//设置线程队列的连接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //设置一直保持活动连接状态
                    .childHandler(new ChannelInitializer<SocketChannel>() {//设置一个通道测试对象
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //给pipeline设置通道处理器
                            ch.pipeline().addLast(new NettyServerHandler());
                        }
                    });//给 workerGroup 的EventLoop对应的管道设置处理器
            //启动服务器，并绑定端口并且同步
            ChannelFuture channelFuture = serverBootstrap.bind(6999).sync();

            //给 channelFuture 注册监听器，监听关心的事件
//            channelFuture.addListener((future) -> {
//                if (future.isSuccess()) {
//                    System.out.println("监听端口成功。。。");
//                } else {
//                    System.out.println("监听端口失败。。。");
//                }
//            });
            //对关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```
#### 4.服务端自定义处理器
```java
/**
 * 自定义一个handler,需要继承netty规定好的某个 HandlerAdapter （规范）
 * 这是自定义的handler才能称之为handler
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * 读取客户端发送的数据
     *
     * @param ctx 上下文对象，含有管道 pipeline,通道 channel, 地址
     * @param msg 客户端发送的数据，默认为Object
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

/*        // 可以将耗时长的业务添加到 taskQueue中执行
        ctx.channel().eventLoop().execute(() -> {
            //处理业务
        });*/
        System.out.println("服务器当前线程："+Thread.currentThread().getName());
        //将msg转为 ByteBuf
        //ByteBuf是netty提供，不是NIO中的 ByteBuffer
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("客户端发送消息是：" + buf.toString(CharsetUtil.UTF_8));
        System.out.println("客户端的远程地址：" + ctx.channel().remoteAddress());
    }

    //数据读取完毕
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //将数据写入到缓存并刷新
        //一般要对数据进行编码
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello~,客户端", CharsetUtil.UTF_8));
    }

    //处理异常，一般需要关闭通道
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
```

## netty使用WebSocket(长连接)
#### 1.服务端
```java
/**
 * 服务端
 * netty 实现 websocket 全双工通信
 */
public class websocket {
    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //创建服务端的启动对象，并使用链式编程来设置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup) //设置两个线程组
                    .channel(NioServerSocketChannel.class)//使用NioServerSocketChannel 作为服务器的通道实现
                    .option(ChannelOption.SO_BACKLOG, 128)//设置线程队列的连接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //设置一直保持活动连接状态
                    .handler(new LoggingHandler(LogLevel.INFO)) //给bossGroup 添加一个日志处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {//设置一个通道测试对象
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //给pipeline设置通道处理器
                            final ChannelPipeline pipeline = ch.pipeline();
                            //因为基于http协议，加入http的编码和解码器
                            pipeline.addLast(new HttpServerCodec());
                            //是以块方式写，添加ChunkedWriteHandler处理器
                            pipeline.addLast(new ChunkedWriteHandler());
                            //http 数据在传输过程中是分段，当浏览器发送大量数据时，就会发送多次http请求
                            //HttpObjectAggregator可以将多个段聚合
                            pipeline.addLast(new HttpObjectAggregator(8192));
                            /**
                             *websocket 数据是以帧（frame）的形式传递
                             * 可以看到WebSocketFrame 下面有六个子类
                             * WebSocketServerProtocolHandler 的核心功能是将http协议升级为ws协议，保持长连接
                             */
                            pipeline.addLast(new WebSocketServerProtocolHandler("/hello"));
                            //自定义handler,处理业务逻辑
                            pipeline.addLast(new MyTextWebSocketFrameHandler());

                        }
                    });//给 workerGroup 的EventLoop对应的管道设置处理器
            //启动服务器，并绑定端口并且同步
            ChannelFuture channelFuture = serverBootstrap.bind(7000).sync();

            //给 channelFuture 注册监听器，监听关心的事件
//            channelFuture.addListener((future) -> {
//                if (future.isSuccess()) {
//                    System.out.println("监听端口成功。。。");
//                } else {
//                    System.out.println("监听端口失败。。。");
//                }
//            });
            //对关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

#### 2.服务端自定义WebSocket处理器
```java
/**
 * TextWebSocketFrame 表示一个文本帧
 */
public class MyTextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        System.out.println("服务器收到消息："+msg.text());
        ctx.channel().writeAndFlush(new TextWebSocketFrame("服务器收到："+msg.text()));
    }

    //客户端连接后，就会触发该函数
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerAdded 触发 "+ctx.channel().id().asLongText());
        System.out.println("handlerAdded 触发 "+ctx.channel().id().asShortText());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerRemoved 触发了。。");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("异常发生");
        ctx.close();
    }
}
```

#### 3.编写 html 客户端
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>

<textarea id="message" style="width: 300px;height: 300px;"></textarea>
<button onclick="send()">发送</button>
<textarea id="response" style="width: 300px;height: 300px;"></textarea>
<button onclick="document.getElementById('response').value=''">清空</button>

<script>
    let socket
    if (window.WebSocket) {
        socket = new WebSocket("ws://localhost:7000/hello")

        socket.onmessage = function (e) {
            let text = document.getElementById('response')
            text.value = text.value + "\n" + e.data
        }
        socket.onopen = function (e) {
            let text = document.getElementById('response')
            text.value = "start...."
        }
        socket.onclose = function (e) {
            let text = document.getElementById('response')
            text.value = text + "\n" + "连接关闭"
        }
    } else {
        alert("浏览器不支持websocket")
    }

    function send() {
        console.log("222")
        if (!socket) {
            console.log("3")

            return
        }
        if (socket.readyState == WebSocket.OPEN) {
            let message = document.getElementById("message").value
            console.log("11")
            console.log(message)
            socket.send(message)
        } else {
            alert("连接未开始。。")
        }
    }
</script>
</body>
</html>
```

## netty使用http(短连接)
#### 1.服务端
```java
public class TestServer {
    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //创建服务端的启动对象，并使用链式编程来设置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup) //设置两个线程组
                    .channel(NioServerSocketChannel.class)//使用NioServerSocketChannel 作为服务器的通道实现
                    .option(ChannelOption.SO_BACKLOG, 128)//设置线程队列得到连接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //设置保持活动连接状态
                    .childHandler(new TestServerInitializer());//给 workerGroup 的EventLoop对应的管道设置处理器
            //启动服务器，并绑定端口并且同步
            ChannelFuture channelFuture = serverBootstrap.bind(6991).sync();
            //对关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

```

```java
public class TestServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //向管道加入处理器
        //获取管道
        ch.pipeline()
                .addLast("myHttpServerCodec",new HttpServerCodec())//添加一个netty提供的处理http的编码-解码器
                .addLast(new TestHttpServerHandler());//添加一个自定义的handler
    }
}
```

```java
/**
 * HttpObject  客户端和服务端相互通讯的数据
 */
public class TestHttpServerHandler extends SimpleChannelInboundHandler<DecoderResultProvider> {
    //读取客户端数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DecoderResultProvider msg) throws Exception {
        if (msg instanceof HttpRequest) {
            System.out.println("msg 类型：" + msg.getClass());
            HttpRequest httpRequest = (HttpRequest) msg;
            System.out.println("uri:  " + httpRequest.uri());
            //过滤特定资源
            if ("/favicon.ico".equals(httpRequest.uri())) {
                System.out.println("请求是 /favicon.ico ，不做响应");
                return;
            }
            //回复消息给浏览器 （http协议）
            ByteBuf content = Unpooled.copiedBuffer("hello,我是服务器。。", CharsetUtil.UTF_8);

            //构建一个 http response
            DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK, content);
            defaultFullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
            defaultFullHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            //将构建好的 response 返回
            ctx.writeAndFlush(defaultFullHttpResponse);

        }
    }
}
```

#### 2.客户端直接用浏览器访问服务端的 6991 端口
## netty实现简单群聊系统
#### 1.服务端启动类
```java
/**
 * netty 实现群聊
 * 服务端可以监听客户端的上线，离线和转发消息
 * 客户端可以发送消息和接收其它客户端发送的消息
 */
public class GroupChatServer {

    private int port;

    public GroupChatServer(int port) {
        this.port = port;
    }

    public void run() throws InterruptedException {

        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new StringDecoder())//向pipeline加入解码器
                                    .addLast(new StringEncoder())//想pipeline加入编码器
                                    .addLast("myServerHandler", new ServerHandler());
                        }
                    });
            final ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new GroupChatServer(7000).run();
    }
}
```
#### 2.服务端自定义处理器
```java
public class ServerHandler extends SimpleChannelInboundHandler<String> {

    // 定义一个 channel 组，管理所有的channel
    //GlobalEventExecutor.INSTANCE 全局的事件执行器，是一个单例
    // 必须用 static 修饰，表示类变量
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 表示连接建立，一旦连接，第一个被执行
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //将该客户端加入聊天通知给其它客户端
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String s = String.format("客户端 %s 加入聊天 %s", ctx.channel().remoteAddress(), dateTime);
        //该方法会自动遍历所有channel，并发送消息，不需要自己遍历
//        channelGroup.writeAndFlush(s);
        channelGroup.writeAndFlush(s);
        //将该客户端的channel 加入channel 组
        channelGroup.add(ctx.channel());
    }

    //断开连接，handlerRemoved 执行后，会自动将当前channel从channelGroup移除
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.channel();
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String s = String.format("客户端 %s 离开了 %s", ctx.channel().remoteAddress(), dateTime);
        //该方法会自动遍历所有channel，并发送消息，不需要自己遍历
        channelGroup.writeAndFlush(s);
    }

    //表示channel处于活动状态
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + " 上线了。。");
    }

    //表示channel处于非活动状态
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "离线了。。");
    }

    //读取数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        final Channel channel = ctx.channel();
        channelGroup.forEach(ch -> {
            if (channel != ch) {
                ch.writeAndFlush("客户端 " + channel.remoteAddress() + " 发送了消息：" + msg);
            } else {
                ch.writeAndFlush("自己发送了消息：" + msg);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}

```
#### 3.客户端启动类
```java
public class GroupChatClient {
    private String ip;
    private int port;

    public GroupChatClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void run() {
        NioEventLoopGroup group = new NioEventLoopGroup(1);

        try {
            final Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new StringDecoder())
                                    .addLast(new StringEncoder())
                                    .addLast(new ClientHandler());
                        }
                    });
            final ChannelFuture sync = bootstrap.connect(ip, port).sync();
            final Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()){
                final String s = scanner.nextLine();
                sync.channel().writeAndFlush(s);
            }
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

    }

    public static void main(String[] args) {
        new GroupChatClient("127.0.0.1",7000).run();
    }
}
```
#### 4.客户端自定义处理器
```java
public class ClientHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println(msg);
    }
}
```

> 1. [netty实现简单的RPC调用框架](https://www.cnblogs.com/yloved/p/12940619.html)   
> 2. [代码托管地址](https://github.com/1612480331/NettyLearn/tree/master/code/src/main/java/com/yls/netty)
