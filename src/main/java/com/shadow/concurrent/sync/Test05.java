package com.shadow.concurrent.sync;

/**
 * @author shadow
 * @create 2020-08-30
 * @description
 *
 * 锁粗化：
 * JIT编译器在执行动态编译时，若发现前后相邻的 synchronized 块使用的是同一个锁对象，那么它会把这几个 synchronized 块
 * 给合并为一个较大的同步块，这样做的好处在于线程在执行这些代码时，就无需频繁申请与释放锁了，从而达到申请与释放锁一次，
 * 就可以执行完全部的同步代码块，从而提升了性能
 */
public class Test05 {

	private Object object = new Object();

	public void method02() {
		// Object object = new Object();
		synchronized (object) {
			System.out.println("hello");
		}
		synchronized (object) {
			System.out.println("world");
		}
		synchronized (object) {
			System.out.println("hello world");
		}
	}
}
