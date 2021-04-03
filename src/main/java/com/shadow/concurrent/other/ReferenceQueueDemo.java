package com.shadow.concurrent.other;

import com.shadow.utils.ConsolePrinter;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class ReferenceQueueDemo {
    public static void main(String[] args) {
        Object o1 = new Object();
        ReferenceQueue queue = new ReferenceQueue();
        WeakReference<Object> weakReference = new WeakReference<>(o1,queue);
        ConsolePrinter.printlnCyan(o1); // java.lang.Object@4617c264
        ConsolePrinter.printlnCyan(weakReference.get());// java.lang.Object@4617c264
        ConsolePrinter.printlnCyan(queue.poll()); // null
        ConsolePrinter.printlnCyan("========================");
        o1 = null;
        System.gc();
        try {TimeUnit.SECONDS.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
        ConsolePrinter.printlnCyan(o1); // null
        ConsolePrinter.printlnCyan(weakReference.get()); // null
        ConsolePrinter.printlnCyan(queue.poll()); // java.lang.ref.WeakReference@36baf30c
    }
}
