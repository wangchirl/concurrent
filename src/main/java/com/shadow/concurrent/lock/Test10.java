package com.shadow.concurrent.lock;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.Exchanger;

/**
 * @author shadow
 * @create 2020-09-13
 * @description
 */
public class Test10 {

	static Exchanger<String> exchanger = new Exchanger<>();

	public static void main(String[] args) {

		new Thread(() ->{
			String s = "t1";
			try {
				s = exchanger.exchange(s);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ConsolePrinter.printlnCyan(Thread.currentThread().getName() + " " + s);
	},"T1").start();


		new Thread(() ->{
			String s = "t2";
			try {
				s = exchanger.exchange(s);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ConsolePrinter.printlnCyan(Thread.currentThread().getName() + " " + s);
		},"T2").start();
	}
}
