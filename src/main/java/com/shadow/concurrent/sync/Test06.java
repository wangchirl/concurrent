package com.shadow.concurrent.sync;

import com.shadow.utils.ConsolePrinter;

/**
 * @author shadow
 * @create 2020-09-09
 * @description
 *
 *     1、死锁：线程1等待线程2互斥持有的资源，而线程2也在等待线程1互斥持有的资源，两个线程都无法继续执行
 *     2、活锁：线程持续重试一个总是失败的操作，导致无法继续执行
 *     3、饿死：线程一直被调度器延迟访问其赖以执行的资源，也许是调度器先于低优先级的线程而执行高优先级的线程，同时总是会有一个
 *             高优先级的线程可以执行，饿死也叫做无限延迟
 *
 *     检测线程死锁的方法：
 *     1、可视化工具
 *     	① jvisualvm
 *     	② jconsole
 *     	③ jmc
 *	   2、命令行
 *	   	 jps -l ：找到 java程序进程号
 *	   	 jstack pid : 查看线程栈信息
 *
 */
public class Test06 {

	private Object lock1 = new Object();
	private Object lock2 = new Object();


	public void method1() {
		synchronized (lock1){
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			synchronized (lock2){
				ConsolePrinter.printlnCyan("method1  invoked...");
			}
		}
	}

	public void method2() {
		synchronized (lock2) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			synchronized (lock1){
				ConsolePrinter.printlnCyan("method2 invoked...");
			}
		}
	}

	public static void main(String[] args) {
		// 死锁
		Test06 test06 = new Test06();
		new Thread(() ->{
			test06.method1();
		}).start();

		new Thread(() ->{
			test06.method2();
		}).start();

	}
}
