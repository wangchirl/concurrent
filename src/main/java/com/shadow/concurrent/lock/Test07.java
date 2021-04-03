package com.shadow.concurrent.lock;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * @author shadow
 * @create 2020-09-13
 * @description
 *
 *  对于CAS来说，其操作数主要涉及到如下三个：
 *   1.需要被操作的内存值V（主内存值）
 *   2.需要进行比较的值A（期望的值，即当前线程读取到的值）
 *   3.需要进行写入的值B（将要修改为的值）
 *  只有当V==A的时候，CAS才会通过原子操作的手段来将V的值更新成功，并且返回旧值
 *
 *  关于CAS的限制或问题：
 *   1.循环开销问题：并发量大的情况下会导致线程一直自旋
 *   2.只能保证一个变量的原子操作：可以通过AtomicReference来实现对多个变量的原子操作
 *   3.ABA问题：1 -> 2 -> 1，经过修改之后再改回来的问题
 *
 *   ABA问题的解决办法：使用带版本号的原子引用 AtomicStampedReference
 *
 */
public class Test07 {
	public static void main(String[] args) throws InterruptedException {
		AtomicInteger atomicInteger = new AtomicInteger(5);

		ConsolePrinter.printlnCyan(atomicInteger.get());
		// 返回旧值
		ConsolePrinter.printlnCyan(atomicInteger.getAndSet(8));

		ConsolePrinter.printlnCyan(atomicInteger.get());

		ConsolePrinter.printlnCyan(atomicInteger.getAndIncrement());

		ConsolePrinter.printlnCyan(atomicInteger.get());

		// ABA 问题
		AtomicReference<Integer> atomicReference = new AtomicReference<>(100);
		AtomicStampedReference<Integer> atomicStampedReference = new AtomicStampedReference<Integer>(100,1);
		ConsolePrinter.printlnCyan("===============ABA问题的产生====================");
		new Thread(() -> {
			ConsolePrinter.printlnCyan(atomicReference.compareAndSet(100, 101));
			ConsolePrinter.printlnCyan(atomicReference.compareAndSet(101, 100));
		},"T1").start();
		new Thread(() -> {
			try {TimeUnit.SECONDS.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
			ConsolePrinter.printlnCyan(atomicReference.compareAndSet(100,1) + "\t" + atomicReference.get());
		},"T2").start();
		try {TimeUnit.SECONDS.sleep(2);} catch (InterruptedException e) {e.printStackTrace();}

		ConsolePrinter.printlnCyan("===============ABA问题的解决====================");
		new Thread(() -> {
			int stamp = atomicStampedReference.getStamp();
			ConsolePrinter.printlnCyan("第一次版本号：" + stamp);
			try {TimeUnit.SECONDS.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
			atomicStampedReference.compareAndSet(100,101,stamp,stamp+1);
			ConsolePrinter.printlnCyan("第二次版本号：" + atomicStampedReference.getStamp());
			atomicStampedReference.compareAndSet(101,100,atomicStampedReference.getStamp(),atomicStampedReference.getStamp()+1);
			ConsolePrinter.printlnCyan("第三次版本号：" + atomicStampedReference.getStamp());
		},"T3").start();
		new Thread(() -> {
			int stamp = atomicStampedReference.getStamp();
			ConsolePrinter.printlnCyan("第一次版本号：" + stamp);
			try {TimeUnit.SECONDS.sleep(3);} catch (InterruptedException e) {e.printStackTrace();}
			ConsolePrinter.printlnCyan(atomicStampedReference.compareAndSet(100, 10, stamp, stamp + 1) + "\t" + atomicStampedReference.getStamp() +"\t" + atomicStampedReference.getReference());
		},"T3").start();
	}
}
