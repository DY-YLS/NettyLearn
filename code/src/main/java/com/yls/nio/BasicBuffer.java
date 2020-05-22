package com.yls.nio;

import java.nio.IntBuffer;

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
