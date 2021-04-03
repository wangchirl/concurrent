package com.shadow.concurrent.exercise;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.*;
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
		// 方式4 - CountDownLatch
		//m4();
		// 方式5 - 阻塞队列 - LinkedBlockingQueue
		//m5();
		// 方式6 = Exchanger - 不是很可靠（不能保证交换后的打印顺序）
		//【m6();】
		// 方式7 - TransferQueue - LinkedTransferQueue
		//m7();
	}

	public static void m7() {

		LinkedTransferQueue<Character> queue = new LinkedTransferQueue<>();

		t1 = new Thread(() ->{
			for (char c : s1) {
				try {
					queue.transfer(c);
					ConsolePrinter.printlnCyan(queue.take()); // take()阻塞方法
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t2 = new Thread(() ->{
			for (char c : s2) {
				try {
					ConsolePrinter.printlnCyan(queue.take());
					queue.transfer(c);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t2.start();
		t1.start();

	}

	public static void m6() {
		Exchanger<String> exchanger = new Exchanger<>();

		t1 = new Thread(() ->{
			for (char c : s1) {
				try {
					ConsolePrinter.printlnCyan(c);
					exchanger.exchange("T1");
					TimeUnit.MILLISECONDS.sleep(200); // 不睡眠不能保证交换后先打印t1还是t2
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t2 = new Thread(() ->{
			for (char c : s2) {
				try {
					exchanger.exchange("T2");
					ConsolePrinter.printlnCyan(c);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t1.start();
		t2.start();

	}


	public static void m5() {
		LinkedBlockingQueue<String> queue1 = new LinkedBlockingQueue<>();
		LinkedBlockingQueue<String> queue2 = new LinkedBlockingQueue<>();

		t1 = new Thread(() ->{
			for (char c : s1) {
				try {
					queue1.take();
					ConsolePrinter.printlnCyan(c);
					queue2.put("ok");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t2 = new Thread(() ->{
			for (char c : s2) {
				try {
					queue1.put("ok");
					queue2.take();
					ConsolePrinter.printlnCyan(c);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t1.start();
		t2.start();

	}


	public static void m4() {

		AtomicReference<CountDownLatch> latch1 = new AtomicReference<>(new CountDownLatch(1));
		AtomicReference<CountDownLatch> latch2 = new AtomicReference<>(new CountDownLatch(1));

		t1 = new Thread(() ->{
			for (char c : s1) {
				ConsolePrinter.printlnCyan(c);
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
					ConsolePrinter.printlnCyan(c);
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
					ConsolePrinter.printlnCyan(c);
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
					ConsolePrinter.printlnCyan(c);
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
						ConsolePrinter.printlnCyan(c);
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
						ConsolePrinter.printlnCyan(c);
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
				ConsolePrinter.printlnCyan(c);
				LockSupport.unpark(t2);
				LockSupport.park();
			}
		});

		t2 = new Thread(() ->{
			for (char c : s2) {
				LockSupport.park();
				ConsolePrinter.printlnCyan(c);
				LockSupport.unpark(t1);
			}
		});
		t1.start();
		t2.start();
	}
}
