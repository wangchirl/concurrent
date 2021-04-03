package com.shadow.concurrent.lock;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.Semaphore;

/**
 * @author shadow
 * @create 2020-09-13
 * @description
 */
public class Test08 {
	public static void main(String[] args) {
		//Semaphore semaphore = new Semaphore(1);
		Semaphore semaphore = new Semaphore(1,false);

		for (int i = 1; i <=10 ; i++) {
			int finalI = i;
			new Thread(() -> {
				try {
					semaphore.acquire();
					// 业务逻辑
					ConsolePrinter.printlnCyan(finalI + " 抢到车位了！");
					Thread.sleep(1000);
					ConsolePrinter.printlnCyan(finalI + " 离开车位了！");
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					semaphore.release();
				}
			},String.valueOf(i)).start();
		}
	}
}
