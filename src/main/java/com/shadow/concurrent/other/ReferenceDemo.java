package com.shadow.concurrent.other;

import com.shadow.utils.ConsolePrinter;

public class ReferenceDemo {
    public static void main(String[] args) {
        Object o1 = new Object();
        Object o2 = o1;
        o1 = null;
        System.gc();
        ConsolePrinter.printlnCyan(o2);
    }
}
