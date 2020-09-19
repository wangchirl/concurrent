package com.shadow.concurrent.other;

import java.lang.ref.SoftReference;

public class SoftReferenceDemo {
    public static void main(String[] args) {
//        softRefMemoryEnough();
        softRefMemoryNotEnough();
    }

    public static void softRefMemoryEnough(){
        Object o1 = new Object();
        SoftReference<Object> softReference = new SoftReference<>(o1);
        System.out.println(o1); // java.lang.Object@4617c264
        System.out.println(softReference.get()); // java.lang.Object@4617c264

        o1 = null;
        System.out.println(o1); // null
        System.out.println(softReference.get()); // java.lang.Object@4617c264
    }

    /**
     * -Xmx5m -Xms5m -XX:+PrintGCDetails
     */
    public static void softRefMemoryNotEnough(){
        Object o1 = new Object();
        SoftReference<Object> softReference = new SoftReference<>(o1);
        System.out.println(o1); // java.lang.Object@4617c264
        System.out.println(softReference.get()); // java.lang.Object@4617c264

        o1 = null;
        try {
            byte[] bytes = new byte[20*1024*1024];
        } catch (Throwable e){
            System.out.println(o1); // null
            System.out.println(softReference.get()); // null
        }
    }
}
