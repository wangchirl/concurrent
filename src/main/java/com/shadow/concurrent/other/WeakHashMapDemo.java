package com.shadow.concurrent.other;

import com.shadow.utils.ConsolePrinter;

import java.util.HashMap;
import java.util.WeakHashMap;

public class WeakHashMapDemo {
    public static void main(String[] args) {
        map();
        weakMap();
    }

    public static void map(){
        HashMap<Integer,String> map = new HashMap<>();
        Integer key = new Integer(1);
        String value = "HashMap";
        map.put(key,value);
        ConsolePrinter.printlnCyan(map); // {1=HashMap}
        key = null;
        System.gc();
        ConsolePrinter.printlnCyan(map); // {1=HashMap}
    }

    public static void weakMap(){
        WeakHashMap<Integer,String> wmap = new WeakHashMap();
        Integer key = new Integer(1);
        String value = "HashMap";
        wmap.put(key,value);
        ConsolePrinter.printlnCyan(wmap); // {1=HashMap}
        key = null;
        System.gc();
        ConsolePrinter.printlnCyan(wmap); // {}
    }
}
