package com.shadow.concurrent.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author shadow
 * @create 2020-09-12
 * @description
 */
public class Test04 {
	public static void main(String[] args) {
		// 不可重用
		CountDownLatch countDownLatch = new CountDownLatch(3);

		IntStream.range(0,3).forEach(i -> new Thread(() ->{
			try {
			    Thread.sleep(2000);
				System.out.println("sportor "+ i + " arrived...");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
			 	countDownLatch.countDown();
			}
		}).start());

		try {
			countDownLatch.await();
			//countDownLatch.await(200,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("比赛结束！");
	}
}
