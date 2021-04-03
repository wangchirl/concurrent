package com.shadow.concurrent.other;

import com.shadow.utils.ConsolePrinter;

import java.lang.ref.WeakReference;

public class WeakReferenceDemo {
    public static void main(String[] args) {
        Object o1 = new Object();
        WeakReference weakReference = new WeakReference(o1);
        ConsolePrinter.printlnCyan(o1);
        ConsolePrinter.printlnCyan(weakReference.get());

        o1 = null;
        System.gc();
        ConsolePrinter.printlnCyan(o1);
        ConsolePrinter.printlnCyan(weakReference.get());
    }
}
