package com.shadow.concurrent.future;

import java.util.Random;
import java.util.concurrent.*;

/**
 * @author shadow
 * @create 2020-09-16
 * @description
 */
public class Test01 {
	public static void main(String[] args) throws Exception {

		Callable<Integer> callable = () ->{

			System.out.println("pre ...");
			 Thread.sleep(3000);
			int nextInt = new Random().nextInt(1000);

			System.out.println("post ...");
			return nextInt;
		};

		FutureTask<Integer> futureTask = new FutureTask<>(callable);

		new Thread(futureTask).start();

		Thread.sleep(1000);
		System.out.println("main thread...");

		System.out.println(futureTask.get()); // get()被阻塞
		System.out.println(futureTask.get(1,TimeUnit.SECONDS)); // 超时异常，但是子线程的任务不受影响，会继续完成

	}
}
