package com.yls.netty.dubborpc.netty;

/**
 * rpc调用时传输类的信息
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
