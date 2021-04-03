package com.shadow.concurrent.sync;

import com.shadow.utils.ConsolePrinter;

/**
 * @author shadow
 * @create 2020-08-30
 * @description
 *
 * 编译器对于锁的优化措施：
 * 锁消除技术：
 * JIT 编译器（Just In Time编译器）可以在动态编译同步代码时，使用一种叫做逃逸分析的技术，来通过该项技术判别程序中锁使用
 * 的锁对象是否只被一个线程锁使用，而没有散布到其他线程当中；如果情况就是这样的话，那么JIT编译器在编译这个同步代码时就不会
 * 生成 synchronized 关键字锁标识的锁的申请与释放机器码，从而消除了锁的使用流程
 */
public class Test04 {

	// private Object object = new Object();

	public void method01() {
		Object object = new Object();
		synchronized (object) {
			ConsolePrinter.printlnCyan("hello world");
		}
	}
}
