package com.shadow.concurrent.threadpool;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author shadow
 * @create 2020-09-20
 * @description
 */
public class Test03 {
	public static void main(String[] args) throws IOException {
		ExecutorService service = Executors.newWorkStealingPool();
		System.out.println(Runtime.getRuntime().availableProcessors());

		service.execute(new R(1));
		service.execute(new R(2));
		service.execute(new R(2));
		service.execute(new R(2));
		service.execute(new R(2));

		System.in.read();

	}

	static class R implements Runnable {
		int time;

		R(int time) {
			this.time = time;
		}

		@Override
		public void run() {
			try {
				TimeUnit.SECONDS.sleep(time);
				System.out.println(Thread.currentThread().getName() + " execute task");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
