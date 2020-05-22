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