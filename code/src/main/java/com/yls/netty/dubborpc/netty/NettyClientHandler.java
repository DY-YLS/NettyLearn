package com.yls.netty.dubborpc.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
