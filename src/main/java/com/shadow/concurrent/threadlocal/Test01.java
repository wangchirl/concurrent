package com.shadow.concurrent.threadlocal;

import java.util.concurrent.TimeUnit;

/**
 * @author shadow
 * @create 2020-09-16
 * @description
 */
public class Test01 {

	private static ThreadLocal<String> threadLocal1 = new ThreadLocal<>();
	private static ThreadLocal<String> threadLocal2 = new ThreadLocal<>();

	public static void main(String[] args) throws InterruptedException {

		//ThreadLocal<String> threadLocal1 = new ThreadLocal<>();

		threadLocal1.set("hello");
		threadLocal2.set("are you ok");

		System.out.println(Thread.currentThread().getName() + " => " + threadLocal1.get());
		System.out.println(Thread.currentThread().getName() + " => " + threadLocal2.get());

		threadLocal1.set("world");
		threadLocal2.set("i am ok");
		System.out.println(Thread.currentThread().getName() + " => " + threadLocal1.get());
		System.out.println(Thread.currentThread().getName() + " => " + threadLocal2.get());


		new Thread(() ->{
			threadLocal1.set("this is static");
			System.out.println(Thread.currentThread().getName() + " => " + threadLocal1.get());
			threadLocal2.set("r u ok");
			System.out.println(Thread.currentThread().getName() + " => " +threadLocal2.get());
		},"t1").start();

		TimeUnit.SECONDS.sleep(2);

		System.out.println(Thread.currentThread().getName() + " => " + threadLocal1.get());
		System.out.println(Thread.currentThread().getName() + " => " + threadLocal2.get());

	}
}
