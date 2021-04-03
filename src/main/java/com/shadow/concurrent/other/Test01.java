package com.shadow.concurrent.other;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author shadow
 * @create 2020-09-13
 * @description
 *
 *  关于 synchronized、Atomic原子类、LongAdder的性能测试
 *
 */
public class Test01 {
	final Object lock = new Object();
	static long count1 = 0L;
	static AtomicLong count2 = new AtomicLong(0L);
	static LongAdder count3 = new LongAdder();

	public static void main(String[] args) throws InterruptedException {
		Thread[] threads = new Thread[1000];
		// synchronized
		Test01 test01 = new Test01();
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() ->{
				for (int j = 0; j < 100000; j++) {
					test01.increment();
				}
			});
		}
		long start = System.currentTimeMillis();
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		ConsolePrinter.printlnCyan("值为："+ count1 +" sync 耗时：" + (System.currentTimeMillis() - start));

		// atomic
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() ->{
				for (int j = 0; j < 100000; j++) {
					count2.getAndIncrement();
				}
			});
		}
		start = System.currentTimeMillis();
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		ConsolePrinter.printlnCyan("值为："+ count2.get() +" atomic 耗时：" + (System.currentTimeMillis() - start));

		// longAdder
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() ->{
				for (int j = 0; j < 100000; j++) {
					count3.increment();
				}
			});
		}
		start = System.currentTimeMillis();
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		ConsolePrinter.printlnCyan("值为："+ count3.longValue() +" longAdder 耗时：" + (System.currentTimeMillis() - start));



	}

	public void increment() {
		synchronized (lock){
			count1++;
		}
	}
}
