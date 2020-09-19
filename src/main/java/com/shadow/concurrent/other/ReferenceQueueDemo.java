package com.shadow.concurrent.other;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class ReferenceQueueDemo {
    public static void main(String[] args) {
        Object o1 = new Object();
        ReferenceQueue queue = new ReferenceQueue();
        WeakReference<Object> weakReference = new WeakReference<>(o1,queue);
        System.out.println(o1); // java.lang.Object@4617c264
        System.out.println(weakReference.get());// java.lang.Object@4617c264
        System.out.println(queue.poll()); // null
        System.out.println("========================");
        o1 = null;
        System.gc();
        try {TimeUnit.SECONDS.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
        System.out.println(o1); // null
        System.out.println(weakReference.get()); // null
        System.out.println(queue.poll()); // java.lang.ref.WeakReference@36baf30c
    }
}
