package com.yls.netty.dubborpc.provider;

import com.yls.netty.dubborpc.publicClass.HelloService;
import com.yls.netty.dubborpc.publicClass.Result;

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
