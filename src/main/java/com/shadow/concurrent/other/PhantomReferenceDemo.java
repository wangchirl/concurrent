package com.shadow.concurrent.other;

import com.shadow.utils.ConsolePrinter;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * @author shadow
 * @create 2020-04-30
 * @description
 */
public class PhantomReferenceDemo {
	public static void main(String[] args) {
		Object o = new Object();
		ReferenceQueue queue = new ReferenceQueue();
		PhantomReference phantomReference = new PhantomReference(o,queue);
		ConsolePrinter.printlnCyan(o);
		ConsolePrinter.printlnCyan(phantomReference.get());
		ConsolePrinter.printlnCyan(queue.poll());
	}
}
