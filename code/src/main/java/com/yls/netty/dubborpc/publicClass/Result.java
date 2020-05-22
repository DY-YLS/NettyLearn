package com.yls.netty.dubborpc.publicClass;

/**
 * 测试返回结果为java bean
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
