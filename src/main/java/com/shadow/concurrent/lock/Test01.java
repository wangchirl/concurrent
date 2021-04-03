package com.shadow.concurrent.lock;

import com.shadow.utils.ConsolePrinter;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author shadow
 * @create 2020-09-09
 * @description
 */
public class Test01 {

	private Lock lock = new ReentrantLock();

	public void method1(){
		try {
		 	lock.lock();
			ConsolePrinter.printlnCyan("method1 invoked....");
			// 3、将sleep放到 method1中，method2可能获取到锁
			// Thread.sleep(new Random().nextInt(2));
		//} catch (InterruptedException e) {
		//	e.printStackTrace();
		} finally {
		    lock.unlock();
		}
	}

	public void method2(){
		try {
			lock.lock();
			ConsolePrinter.printlnCyan("method2 invoked....");
		} finally {
			lock.unlock();
		}

		//2、注释掉 method1中的finally块后，可尝试获取锁
//		try {
//			boolean result = lock.tryLock(800,TimeUnit.MILLISECONDS);
//			if(result) {
//				ConsolePrinter.printlnCyan("get the lock");
//			}else {
//				ConsolePrinter.printlnCyan("can't get the lock");
//			}
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		// 3、将sleep放到 method1中，method2可能获取到锁
		/*finally {
			lock.unlock();
		}*/

	}


	public static void main(String[] args) {
		Test01 test01 = new Test01();
		new Thread(() ->{
			for (int i = 0; i < 10; i++) {
				test01.method1();
				// 3、将sleep放到 method1中，method2可能获取到锁
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// 1、注释掉 method1中的finally块后，这里一次性释放多次
//			for (int i = 0; i < 10; i++) {
//				test01.lock.unlock();
//			}
		}).start();

		new Thread(() ->{
			for (int i = 0; i < 10; i++) {
				test01.method2();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
