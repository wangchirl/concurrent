package com.shadow.concurrent.lock;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * @author shadow
 * @create 2020-09-09
 * @description
 *
 * 传统上，我可以通过synchronized关键字 + wait、notify、notifyAll 来实现多个线程之间的协调与通信，
 * 整个过程都是JVM来帮助我们实现的，开发者无需（也无法）了解底层的实现细节
 *
 * 从JDK5开始，并发包提供了Lock、Condition（await、signal、signalAll）来实现多个线程之间的协调与通信，
 * 整个过程都是由开发者来控制的，而且相比于传统方式，更加灵活，功能也更加强大
 *
 *  Condition 的使用自行查阅 ArrayBlockingQueue 的实现
 *
 *  Thread.sleep 与 await（或者是 Object的wait方法）的本质区别：
 *  > sleep方法本质上不会释放锁，而await会释放锁，并且在 signal 后，还需要重新获得锁才能继续执行（该行为与Object的wait方法完全一致）
 *
 */
public class Test02 {


	/**
	 * [0,1,2,3,4,5,6,7,8,9]
	 * @param args
	 */
	public static void main(String[] args) {

		BoundedContainer container = new BoundedContainer();

		IntStream.range(0,20).forEach(i ->{
			new Thread(() ->{
				try {
					//Thread.sleep(new Random().nextInt(1000) + 500);
					container.put(String.valueOf(i));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}).start();
		});

		IntStream.range(0,20).forEach(i ->{
			new Thread(() ->{
				try {
					//Thread.sleep(new Random().nextInt(2000) + 500);
					container.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}).start();
		});

	}


	static class BoundedContainer {

		private String[] container = new String[10];
		Lock lock = new ReentrantLock();
		Condition notEmptyCondition = lock.newCondition();
		Condition notFullCondition = lock.newCondition();
		int count, putIndex, takeIndex; // 元素个数，下一个存放的索引，下一个获取的索引

		public void put(String element) throws InterruptedException {
			lock.lock();
			try {
			    while (count == container.length) { // 满了
			    	notFullCondition.await();
				}
				container[putIndex] = element; // 存入元素
				if(++putIndex == container.length){ // 存到最后了，重置下标
					putIndex = 0;
				}
				++count; // 元素个数++
				System.out.println("put element：" + Arrays.toString(container));
				notEmptyCondition.signal(); // 唤醒
			} finally {
			    lock.unlock();
			}
		}

		public String take() throws InterruptedException {
			lock.lock();
			try {
			    while (count == 0) { // 元素个数为0
			    	notEmptyCondition.await();
				}
			    String res = container[takeIndex]; // 获取元素
				container[takeIndex] = null; // 置为 null
				if(++takeIndex == container.length) { // 取到最后了，下标重置
					takeIndex = 0;
				}
				--count; // 元素个数--
				System.out.println("take element：" + Arrays.toString(container));
				notFullCondition.signal();
				return res;
			} finally {
			    lock.unlock();
			}
		}

	}

}
