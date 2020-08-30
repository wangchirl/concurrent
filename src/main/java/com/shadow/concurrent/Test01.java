package com.shadow.concurrent;

/**
 * @author shadow
 * @create 2020-08-30
 * @description
 * 1、存在一个对象，该对象有一个int类型的成员变量counter，该变量的初始值为0
 * 2、创建2个线程，其中一个线程对counter+1，另外一个线程对counter-1
 * 3、输出该成员变量counter的值
 * 4、要求：1010101010...
 * 注意点：
 * 1、线程的虚假唤醒=> 使用 while 不能使用 if
 * 2、wait、notify、notifyAll
 */
public class Test01 {
	public static void main(String[] args) throws InterruptedException {

		Counter counter = new Counter();

		MyIncrease increase = new MyIncrease(counter);
		MyIncrease increase1 = new MyIncrease(counter);
		MyDecrease decrease = new MyDecrease(counter);
		MyDecrease decrease1 = new MyDecrease(counter);

		increase.start();
		increase1.start();
		decrease.start();
		decrease1.start();


	}



	static class MyIncrease extends Thread {
		private Counter counter;

		public MyIncrease(Counter counter) {
			this.counter = counter;
		}

		@Override
		public void run() {
			for (int i = 0; i < 30; i++) {
				try {
					Thread.sleep((long) (Math.random() * 1000));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				counter.increase();
			}
		}
	}

	static class MyDecrease extends Thread{

		private Counter counter;

		public MyDecrease(Counter counter) {
			this.counter = counter;
		}

		@Override
		public void run() {
			for (int i = 0; i < 30; i++) {
				try {
					Thread.sleep((long) (Math.random() * 1000));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				counter.decrease();
			}
		}
	}

	static class Counter {

		int counter = 0;

		public synchronized void increase() {
			// if (counter != 0) { // 线程虚假唤醒
			while (counter != 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			counter++;
			System.out.print(counter);
			notify();
		}

		public synchronized void decrease() {
			// if (counter == 0) { // 线程虚假唤醒
			while (counter == 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			counter--;
			System.out.print(counter);
			notify();
		}
	}
}
