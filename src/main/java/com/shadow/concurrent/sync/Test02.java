package com.shadow.concurrent.sync;

import com.shadow.utils.ConsolePrinter;

/**
 * @author shadow
 * @create 2020-08-30
 * @description
 * 1、线程共享变量，并发修改问题
 *
 */
public class Test02 {

	public static void main(String[] args) {

		MyThread myThread = new MyThread();

		Thread t1 = new Thread(myThread);
		Thread t2 = new Thread(myThread);

		t1.start();
		t2.start();
	}

	static class MyThread implements Runnable {

		int x;

		@Override
		public void run() {
			x = 0;
			while (true) {
				try {
					Thread.sleep((long) (Math.random() * 1000));
					ConsolePrinter.printlnCyan("result : " + x++);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(x == 30) {
					break;
				}
			}
		}
	}
}
