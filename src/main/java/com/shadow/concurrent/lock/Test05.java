package com.shadow.concurrent.lock;

import java.util.Random;
import java.util.concurrent.*;

/**
 * @author shadow
 * @create 2020-09-12
 * @description
 *  > 底层执行逻辑：
 *   1.初始化CyclicBarrier中的各种成员变量，包括parties、count和Runnable（可选）
 *   2.当调用await方法时，底层会先检查计数器是否已经归零，如果是的话，那么就首先执行可选的Runnable，接下来开始下一个Generation
 *   3.在下一个分代中，将会重置count值为parties，并且创建新的Generation实例
 *   4.同时会调用Condition的signalAll方法，唤醒所有的在屏障前面等待的线程，让其开始继续执行
 *   5.如果计数器没有归零，那么当前的调用线程将会通过Condition的await方法，在屏障前进行等待
 *   6.以上所有执行流程均在Lock锁的控制范围内，不会出现并发问题（--count）
 */
public class Test05 {
	public static void main(String[] args) throws InterruptedException {
		// 可重用、可接受 Runnable的参数
		// CyclicBarrier cyclicBarrier = new CyclicBarrier(3);
		CyclicBarrier cyclicBarrier = new CyclicBarrier(3,() ->{
			System.out.println("比赛开始！");
		});

		// 不可重用
		CountDownLatch countDownLatch1 = new CountDownLatch(3);
		CountDownLatch countDownLatch2 = new CountDownLatch(3);

		for (int j = 0; j < 2; j++) {
			System.out.println("第" + (j+1) + "轮比赛开始！");
			for (int i = 0; i < 3; i++) {
				int finalI = i;
				int finalJ = j;
				new Thread(() ->{
					try {
						Thread.sleep((long) (Math.random() * 2000));
						int random = new Random().nextInt(100);
						System.out.println("sportor" + finalI + " arrived :" + random);

						cyclicBarrier.await();
						// 超时会抛异常
						//cyclicBarrier.await(20,TimeUnit.MILLISECONDS);

						System.out.println("sportor" + finalI +" running :" + random);
						if(finalJ == 0){
							countDownLatch1.countDown();
						}else {
							countDownLatch2.countDown();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();
			}
			if(j == 0){
				countDownLatch1.await();
			}else {
				countDownLatch2.await();
			}
			System.out.println("第 " + (j+1) + "轮比赛结束！");
		}
	}
}
