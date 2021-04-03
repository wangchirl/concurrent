package com.shadow.concurrent.other;

import com.shadow.utils.ConsolePrinter;

import java.lang.ref.SoftReference;

public class SoftReferenceDemo {
    public static void main(String[] args) {
//        softRefMemoryEnough();
        softRefMemoryNotEnough();
    }

    public static void softRefMemoryEnough(){
        Object o1 = new Object();
        SoftReference<Object> softReference = new SoftReference<>(o1);
        ConsolePrinter.printlnCyan(o1); // java.lang.Object@4617c264
        ConsolePrinter.printlnCyan(softReference.get()); // java.lang.Object@4617c264

        o1 = null;
        ConsolePrinter.printlnCyan(o1); // null
        ConsolePrinter.printlnCyan(softReference.get()); // java.lang.Object@4617c264
    }

    /**
     * -Xmx5m -Xms5m -XX:+PrintGCDetails
     */
    public static void softRefMemoryNotEnough(){
        Object o1 = new Object();
        SoftReference<Object> softReference = new SoftReference<>(o1);
        ConsolePrinter.printlnCyan(o1); // java.lang.Object@4617c264
        ConsolePrinter.printlnCyan(softReference.get()); // java.lang.Object@4617c264

        o1 = null;
        try {
            byte[] bytes = new byte[20*1024*1024];
        } catch (Throwable e){
            ConsolePrinter.printlnCyan(o1); // null
            ConsolePrinter.printlnCyan(softReference.get()); // null
        }
    }
}
