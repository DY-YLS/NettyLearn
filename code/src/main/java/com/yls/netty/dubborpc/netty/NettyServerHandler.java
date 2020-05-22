package com.yls.netty.dubborpc.netty;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


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
