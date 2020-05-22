# netty 实现简单的rpc调用
> **yls**   *2020/5/23*
## netty 实现简单rpc准备
1. 使用netty传输java bean对象，可以使用protobuf，也可以通过json转化
2. 客户端要将调用的接口名称，方法名称，参数列表的类型和值传输到服务端，
可以用动态代理
3. 服务端要对接口和实现类进行映射（或者自定义名称与实现类映射），接收到客户端的数据，使用反射调用相关类的函数
4. 客户端使用callable返回调用的结果，先等待，有数据写回后唤醒线程，赋值返回

## 基于netty编码实现 rpc 调用
> 大致流程：   
>1. netty搭建rpc框架；   
>2. 创建服务消费者和服务提供者的公共接口和类   
>3. 创建服务提供者，启动netty框架的服务端   
>4. 创建服务消费者，启动netty框架的客户端，然后获取调用结果
### 1.首先用netty实现一个rpc框架
###### 1.1 创建客户端调用服务端时传输信息的类
```java
/**
 * rpc调用时传输类的信息
 * 客户端与服务端之间通信，传递信息的媒介
 */
public class ClassInfo {
    //自定义name，一般一个接口有多个实现类的时候使用自定义
    // 或者默认使用接口名称
    private String name;
    private String methodName;
    //参数类型
    private Class[] types;
    //参数列表
    private Object[] params;
    //自定义rpc协议
    private String protocol="#rpc#";

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class[] getTypes() {
        return types;
    }

    public void setTypes(Class<?>[] types) {
        this.types = types;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }
}
```
###### 1.2 创建解决TCP粘包拆包的编解码器
```java
/**
 * 编码器
 * MyMessageEncoder  MyMessageDecoder解决粘包拆包问题
 */
public class MyMessageEncoder extends MessageToByteEncoder<String> {
    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
        //先发送内容长度
        out.writeInt(msg.getBytes().length);
        //发送具体的内容
        out.writeBytes(msg.getBytes());
    }
}
```

```java
/**
 * 解码器
 */
public class MyMessageDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //先读取要接收的字节长度
        final int len = in.readInt();
        final byte[] bytes = new byte[len];
        //再根据长度读取真正的字节数组
        in.readBytes(bytes);
        String s = new String(bytes);
        out.add(s);
    }
}
```

###### 1.3 创建netty客户端以及自定义的处理器
```java
public class NettyClient {
    private static NettyClientHandler nettyClientHandler;
    static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(3, 5, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));

    public static <T> T getBean(Class<T> service) {
        String simpleName = service.getSimpleName();
        return getBean(service, simpleName);
    }

    //获取一个动态代理对象
    public static <T> T getBean(Class<T> service, String name) {
        T o = (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service}, ((proxy, method, args1) -> {
            //先建立连接
            if (nettyClientHandler == null) {
                start(ClientBootStrap.getHost()
                        , ClientBootStrap.getPort());
            }
            //组装传输类的属性值
            ClassInfo classInfo = new ClassInfo();
            classInfo.setName(name);
            classInfo.setMethodName(method.getName());
            Class<?>[] parameterTypes = method.getParameterTypes();
            classInfo.setTypes(parameterTypes);
            classInfo.setParams(args1);
            nettyClientHandler.setClassInfo(classInfo);
            //运行线程，发送数据
            Future future = threadPool.submit(nettyClientHandler);
            //返回结果
            String o1 = (String) future.get();
            ObjectMapper objectMapper = new ObjectMapper();
            //获取返回类型，并将服务端返回的json数据转化为对应的类型
            Type returnType = method.getAnnotatedReturnType().getType();
            Object o2 = objectMapper.readValue(o1, (Class<?>) returnType);
            return o2;
        }));
        return o;
    }

    //启动netty客户端
    public static void start(String host, int port) {
        nettyClientHandler = new NettyClientHandler();
        //客户端需要一个事件循环组就可以
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            //创建客户端的启动对象 bootstrap ，不是 serverBootStrap
            Bootstrap bootstrap = new Bootstrap();
            //设置相关参数
            bootstrap.group(group) //设置线程组
                    .channel(NioSocketChannel.class) //设置客户端通道的实现数 （反射）
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new MyMessageDecoder())
                                    .addLast(new MyMessageEncoder())
                                    .addLast(nettyClientHandler); //加入自己的处理器
                        }
                    });
            System.out.println("客户端 ready is ok..");
            //连接服务器
            final ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            //对关闭通道进行监听
//            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
//            group.shutdownGracefully();
        }
    }
}
```

```java
/**
 * 由于需要在 handler 中发送消息给服务端，并且将服务端返回的消息读取后返回给消费者
 * 所以实现了 Callable 接口，这样可以运行有返回值的线程
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter implements Callable {

    private ClassInfo classInfo; //传递数据的类
    private ChannelHandlerContext context;//上下文
    private Object result;//服务端返回的结果
    private Lock lock = new ReentrantLock();//使用锁将 channelRead和 call 函数同步
    private Condition condition = lock.newCondition();//精准唤醒 call中的等待

    public void setClassInfo(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    //通道连接时，就将上下文保存下来，因为这样其他函数也可以用
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.context = ctx;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelInactive 被调用。。。");
    }

    //当服务端返回消息时，将消息复制到类变量中，然后唤醒正在等待结果的线程，返回结果
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        lock.lock();
        System.out.println(ctx.channel().hashCode());
        System.out.println("收到服务端发送的消息 " + msg);
        result = msg;
        //唤醒等待的线程
        condition.signal();
        lock.unlock();
    }

    //这里面发送数据到服务端，等待channelRead方法接收到返回的数据时，将数据返回给服务消费者
    @Override
    public Object call() throws Exception {
        lock.lock();
        ObjectMapper objectMapper = new ObjectMapper();
        final String s = objectMapper.writeValueAsString(classInfo);
        context.writeAndFlush(s);
        System.out.println("发出数据  " + s);
        //向服务端发送消息后等待channelRead中接收到消息后唤醒
        condition.await();
        lock.unlock();
        return result;
    }

    //异常处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
```

###### 1.4 创建netty服务端以及自定义的处理器
```java
public class NettyServer {
    //启动netty服务端
    public static void start(int port) {
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
                            ch.pipeline()
                                    .addLast(new MyMessageDecoder())
                                    .addLast(new MyMessageEncoder())
                                    .addLast(new NettyServerHandler());
                        }
                    });//给 workerGroup 的EventLoop对应的管道设置处理器
            //启动服务器，并绑定端口并且同步
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            //给 channelFuture 注册监听器，监听关心的事件,异步的时候使用
//            channelFuture.addListener((future) -> {
//                if (future.isSuccess()) {
//                    System.out.println("监听端口成功。。。");
//                } else {
//                    System.out.println("监听端口失败。。。");
//                }
//            });
            //对关闭通道进行监听,监听到通道关闭后，往下执行
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
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    public static Map<String, Class<?>> classNameMapping = new HashMap();

    public static void setClassNameMapping(Object object) {
        Class<?> clazz = object.getClass();
        Class<?>[] interfaces = clazz.getInterfaces();
        Class<?> anInterface = interfaces[0];
        setClassNameMapping(anInterface.getSimpleName(), object);
    }

    //为实现类定义标识，方便客户端和服务端通信调用
    public static void setClassNameMapping(String name, Object object) {
        Class<?> clazz = object.getClass();
        classNameMapping.put(name, clazz);
    }

    //接收客户端传入的值，将值解析为类对象，获取其中的属性，然后反射调用实现类的方法
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String s = (String) msg;
        System.out.println("接收到数据 " + s);
        ObjectMapper objectMapper = new ObjectMapper();
        ClassInfo classInfo = objectMapper.readValue(s, ClassInfo.class);
        //确认是rpc调用才往下执行
        if(classInfo!=null && "#rpc#".equals(classInfo.getProtocol())){
            //反射调用实现类的方法
            String name = classInfo.getName();
            //获取指定名称的实现类
            Class<?> aClass = classNameMapping.get(name);
            Object o = aClass.newInstance();
            if (classInfo.getTypes().length > 0) {
                Method method = aClass.getMethod(classInfo.getMethodName(), classInfo.getTypes());
                method.setAccessible(true);
                Object invoke = method.invoke(o, classInfo.getParams());
                String s1 = objectMapper.writeValueAsString(invoke);
                ctx.writeAndFlush(s1);
            } else {
                Method method = aClass.getMethod(classInfo.getMethodName());
                method.setAccessible(true);
                Object invoke = method.invoke(o);
                String s1 = objectMapper.writeValueAsString(invoke);
                ctx.writeAndFlush(s1);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

### 2.创建服务消费者和服务提供者的公共接口和类
```java
public interface HelloService {
    Result hello(String s);
    String str();
}
```
```java
/**
 * 测试返回结果为java bean时使用的类
 */
public class Result {
    private int id;
    private String content;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
```

### 3.创建服务提供者
###### 3.1 服务提供者实现公共接口
```java
public class HelloServiceImpl implements HelloService {
    @Override
    public Result hello(String s) {
        System.out.println("收到消费者的请求。。" + s);
        Result result=new Result();
        result.setId(1);
        result.setContent("你好,我已经收到了你的消费请求");
        return result;
    }

    @Override
    public String str() {
        return "我是一个字符串。。。";
    }
}
```
###### 3.2 启动netty框架的服务端
```java
public class ServerBootStrap {
    public static void main(String[] args) {
        NettyServerHandler.setClassNameMapping(new HelloServiceImpl());
        NettyServer.start(9999);
    }
}
```

### 4.创建服务消费者，启动netty框架的客户端，然后获取调用结果
```java
/**
 * 消费者
 */
public class ClientBootStrap {
    private static String host = "127.0.0.1";
    private static int port = 9999;

    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    public static void main(String[] args) {
        //连接netty，并获得一个代理对象
        HelloService bean = NettyClient.getBean(HelloService.class);
        //测试返回结果为java bean
        Result res = bean.hello("ffafa");
        System.out.println("res=====" + res.getContent());
        //测试返回结果为 String
        String str = bean.str();
        System.out.println("str=====" + str);
    }
}
```

### 代码托管地址：[https://github.com/1612480331/NettyLearn/tree/master/code/src/main/java/com/yls/netty/dubborpc](https://github.com/1612480331/NettyLearn/tree/master/code/src/main/java/com/yls/netty/dubborpc)


