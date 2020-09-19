package com.shadow.concurrent.threadlocal;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

/**
 * @author shadow
 * @create 2020-09-19
 * @description
 */
public class Test02 {

	static ThreadLocal<Person> threadLocal = new ThreadLocal<>();

	public static void main(String[] args) {
		new Thread(()->{
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println(Thread.currentThread().getName() + " = " + threadLocal.get());// null
		},"t1").start();

		new Thread(()->{
			Person person = new Person();
			person.name = "shadow";
			threadLocal.set(person);

			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println(Thread.currentThread().getName() + " = " + threadLocal.get().name);
		},"t2").start();
	}

	static class Person{
		String name;
	}
}
