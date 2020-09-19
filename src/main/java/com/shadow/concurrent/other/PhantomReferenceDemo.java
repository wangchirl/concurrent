package com.shadow.concurrent.other;

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
		System.out.println(o);
		System.out.println(phantomReference.get());
		System.out.println(queue.poll());
	}
}
