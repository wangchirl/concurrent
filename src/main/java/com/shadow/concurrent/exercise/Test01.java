package com.shadow.concurrent.exercise;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author shadow
 * @create 2020-09-19
 * @description
 * 练习1：多线程A1B2C3...交替打印
 */
public class Test01 {

	static char[] s1 = "ABCDEF".toCharArray();
	static char[] s2 = "123456".toCharArray();
	static Thread t1,t2;

	public static void main(String[] args) throws InterruptedException {
		// 方式1 - LockSupport 最简单
		m1();
		// 方式2 - synchronized + wait/notify/notifyAll
		//m2();
		// 方式3 - Lock + Condition/await/signal/signalAll => 2个 Condition
		//m3();
		// 方式4 - 2个 CountDownLatch
		//m4();
	}

	public static void m4() {

		AtomicReference<CountDownLatch> latch1 = new AtomicReference<>(new CountDownLatch(1));
		AtomicReference<CountDownLatch> latch2 = new AtomicReference<>(new CountDownLatch(1));

		t1 = new Thread(() ->{
			for (char c : s1) {
				System.out.print(c);
				try {
					latch2.get().countDown();
					latch2.set(new CountDownLatch(1));
					latch1.get().await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t2 = new Thread(() ->{
			for (char c : s2) {
				try {
					latch2.get().await();
					System.out.print(c);
					latch1.get().countDown();
					latch1.set(new CountDownLatch(1));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t1.start();
		t2.start();
	}


	public static void m3() throws InterruptedException {
		RC rc = new RC();
		t1 = new Thread(() ->{
			rc.printChar();
		});

		t2 = new Thread(() ->{
			rc.printNum();
		});

		t2.start();// 先启动
		TimeUnit.SECONDS.sleep(1);
		t1.start();
	}

	static class RC {
		ReentrantLock lock = new ReentrantLock();
		Condition condition1 = lock.newCondition();
		Condition condition2 = lock.newCondition();

		public void printChar() {
			try {
			 lock.lock();
				for (char c : s1) {
					System.out.print(c);
					condition2.signal();
					condition1.await();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
			    lock.unlock();
			}
		}

		public void printNum() {
			lock.lock();
			try{
				for (char c : s2) {
					condition2.await();
					System.out.print(c);
					condition1.signal();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
			    lock.unlock();
			}
		}

	}


	public static void m2() {
		// 锁
		final Object lock = new Object();

		t1 = new Thread(() ->{
			synchronized (lock){
					for (char c : s1) {
						System.out.print(c);
						try {
							lock.notify();
							lock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					lock.notify();
			}
		});

		t2 = new Thread(() ->{
			synchronized (lock) {
				for (char c : s2) {
					try {
						lock.wait();
						System.out.print(c);
						lock.notify();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				lock.notify();
			}
		});

		t2.start();// 先启动
		t1.start();

	}


	public static void m1() {
		t1 = new Thread(() ->{
			for (char c : s1) {
				System.out.print(c);
				LockSupport.unpark(t2);
				LockSupport.park();
			}
		});

		t2 = new Thread(() ->{
			for (char c : s2) {
				LockSupport.park();
				System.out.print(c);
				LockSupport.unpark(t1);
			}
		});
		t1.start();
		t2.start();
	}
}
