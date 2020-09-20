package com.shadow.concurrent.threadpool;

import java.util.concurrent.*;

/**
 * @author shadow
 * @create 2020-09-20
 * @description
 */
public class Test01 {
	public static void main(String[] args) {
		// 1.
		Executors.newSingleThreadExecutor();
		// 2.
		Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
		// 3.
		Executors.newCachedThreadPool();
		// 4.
		Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
		// 5.
		Executors.newWorkStealingPool();

		// 自定义
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
				1,
				Runtime.getRuntime().availableProcessors(),
				60L,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(3),
				Executors.defaultThreadFactory(),
//				new ThreadPoolExecutor.AbortPolicy());
//				new ThreadPoolExecutor.DiscardPolicy());
//				new ThreadPoolExecutor.DiscardOldestPolicy());
//				new ThreadPoolExecutor.CallerRunsPolicy());
				new CustomerHandler());

		// 拒绝策略
		for (int i = 0; i < 8; i++) {
			threadPoolExecutor.submit(() ->{
				try {
					System.out.println(Thread.currentThread().getName());
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		threadPoolExecutor.shutdown();
	}

	static class CustomerHandler implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			// log
			System.out.println("log :" + r);
		}
	}
}
