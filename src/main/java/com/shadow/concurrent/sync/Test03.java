package com.shadow.concurrent.sync;

import com.shadow.utils.ConsolePrinter;

/**
 * @author shadow
 * @create 2020-08-30
 * @description
 * 1、synchronized 关键字详解
 * > 修饰成员方法 => 锁的是当前对象
 * > 修饰静态方法 => 锁的是当前对象的class对象
 * 2、字节码层面：
 * > ACC_SYNCHRONIZED
 * > monitorenter
 * > monitorexit
 * 3、修饰代码块
 * > monitorenter
 * > monitorexit
 * > 一个 monitorenter 对应 monitorexit 可能是 1:1（抛异常情况）或1:2（一般情况）
 * 4、修饰方法（成员或静态方法）
 * > ACC_SYNCHRONIZED 访问修饰符来控制
 * 5、JVM中的同步是基于进入与退出监视器对象（管程对象）（Monitor）来实现的
 *    每个对象实例都会有一个Monitor对象，Monitor对象会和Java对象一同创建
 *    与销毁，Monitor对象是由C++来实现的（OpenJDK）
 *
 *    当多个线程同时访问一段同步代码时，这些线程会被放到一个EntryList集合中，
 *    处于阻塞状态的线程都会被放到该列表中，接下来，当线程获取到对象的Monitor时，
 *    Monitor是依赖底层操作系统的mutex lock来实现互斥的，线程获取mutex成功，
 *    则会持有mutex，这时其他线程就无法再获取到该mutex
 *
 *    如果线程调用了wait方法，那么该线程就会释放掉所持有的mutex，并且该线程会进入
 *    到WaitSet集合（等待集合）中，等待下一次被其他线程调用notify/notifyAll唤醒，
 *    如果当前线程顺利执行完毕方法，那么它就会释放掉所持有的mutex
 *
 *    总结：
 *    同步锁在这种实现方式中，业务Monitor是依赖底层操作系统实现，这样就会存在用户态和
 *    内核态之间的切换，所以会增加性能开销
 *
 *    通过对象互斥锁的概念来保证共享数据操作的完整性，每个对象都对应于一个可称为【互斥锁】
 *    的标记，这个标记用于保证在任何时刻只能有一个线程访问该对象
 *
 *    那些处于EntryList与WaitSet中的线程均处于阻塞状态，阻塞操作是由操作系统来完成的，
 *    在linux下是通过 pthread_mutex_lock函数来完成的，线程被阻塞后便会进入到内核调度状态
 *    这会导致系统在用户态与内核态之间来回切换，严重影响锁的性能
 *
 *    解决上述问题的办法便是自旋，其原理是：当发生对Monitor的争用时，若Owner能够在很短的时间内
 *    释放掉锁，则那些争用的线程就可以稍微等待一下（即所谓的自旋），在Owner线程释放锁之后，争用
 *    线程可能会立刻获取到锁，从而避免了系统阻塞。不过，当Owner运行的时间超过了临界值后，争用线程
 *    自旋一段时间后依然无法获取到锁，这时争用线程则会停止自旋而进入到阻塞状态。所以总体的思想是：
 *    不成功再进行阻塞，尽量降低阻塞的可能性，这对那些执行时间很短的代码块来说有极大的性能提升，
 *    显然，自旋在多处理器（多核心）上才有意义
 *
 *
 *	  互斥锁的属性：
 *	  1、PTHREAD_MUTEX_TIMED_NP：这是缺省值，也就是普通锁。当一个线程加锁以后，其他请求锁的线程将
 *	     会形成一个等待队列，并且在解锁后按照优先级获取到锁，这种策略可以确保资源分配的公平性
 *	  2、PTHREAD_MUTEX_RECURSIVE_NP：嵌套锁。允许一个线程对同一个锁成功获取多次，并通过unlock解锁
 *	     如果是不同线程请求，则在加锁线程解锁时重新进行竞争
 *	  3、PTHREAD_MUTEX_ERRORCHECK_NP：检错锁。如果一个线程请求同一个锁，则返回ENEADLK，否则与
 *	     PTHREAD_MUTEX_TIMED_NP类型动作相同，这样就保证了当不允许多次加锁时不会出现最简单情况下的死锁
 *	  4、PTHREAD_MUTEX_ADAPTIVE_NP：适应锁，动作最简单的锁类型，仅仅等待解锁后重新竞争
 *
 *
 *
 * 	  ===================================
 *
 *
 * 	  在JDK1.5之前，我们若想实现线程同步，只能通过 synchronized 关键字这一种方式来达成，在底层，Java也是通过
 * 	  synchronized 关键字来做到数据的原子性维护的；synchronized 关键字是JVM实现的一种内置锁，从底层角度来说，
 * 	  这种锁的获取与释放都是由JVM帮助我们隐式实现的
 *
 * 	  从JDK1.5开始，并发包引入了 Lock 锁，Lock 同步锁是基于Java来实现的，因此锁的获取与释放都是通过Java代码
 * 	  来实现与控制的；然而，synchronized 是基于底层操作系统的 Mutex Lock 来实现的，每次对锁的获取与释放动作
 * 	  都会带来用户态与内核态之间的切换，这种切换会极大的增加系统的负担，在并发量较高时，也就是说锁的竞争比较激
 * 	  烈时，synchronized 锁在性能上的表现就非常差
 *
 * 	  从JDK1.6开始，synchronized 锁的实现发生了比较大的变化，JVM 引入了相应的优化手段来提升 synchronized 锁
 * 	  的性能，这种提升涉及到偏向锁、轻量级锁及重量级锁等，从而减少锁的竞争锁带来的用户态与内核态之间的切换；这种
 * 	  锁的优化实际上是通过Java对象头中的一些标志位来去实现的；对于锁的访问与改变，实际上都是与Java对象头息息相关
 *
 *    从JDK1.6开始，对象实例在堆当中会被划分为三个组成部分：对象头、实例数据与对齐填充
 *
 *    对象头主要由三块内容来构成：
 *    1.Mark Word
 *    2.指向类的指针
 *    3.数组长度（仅限数组对象）
 *
 *    其中Mark Word（它记录了对象、锁及垃圾回收相关的信息，在64位JVM中，其长度也是64bit）的位信息包括了如下组成部分：
 *    1.无锁标记
 *    2.偏向锁标记
 *    3.轻量级锁标记
 *    4.重量级锁标记
 *    5.GC标记
 *
 *    对于 synchronized 锁来说，锁的升级主要都是通过 Mark Word中的锁标志位与是否是偏向锁标志位来达成的，synchronized
 *    关键字所对应的锁都是先从偏向锁开始，随着锁竞争的不断升级，逐步演化至轻量级锁，最后则变成了重量级锁
 *
 *    对于锁的演化来说，它会经历如下阶段：
 *    无锁 -> 偏向锁 -> 轻量级锁（自旋是轻量级锁中的一种） -> 重量级锁
 *
 *    偏向锁：
 *    针对于一个线程来说的，它的主要作用就是优化同一个线程多次获取一个锁的情况；如果一个 synchronized 方法被一个线程访问
 *    那么这个方法所在的对象就会在其 Mark Word中进行偏向锁的标记，同时还会有一个字段来存储该线程的ID；当这个线程再次访问
 *    同一个 synchronized 方法时，它会检查这个对象的 Mark Word的偏向锁标记以及是否指向了其线程ID，如果是的话，那么该线程
 *    就无需再去进入管程（Monitor）了，而是直接进入到该方法体中。
 *    如果是另外一个线程访问这个 synchronized 方法，那么偏向锁会被取消掉
 *
 *    轻量级锁：
 *    若一个线程已经获取到了当前对象的锁，这时第二个线程又开始尝试争抢该对象的锁，由于该对象的锁已经被第一个线程获取到，因此
 *    它是偏向锁，而第二个线程在争抢时，会发现该对象头中的Mark Word 已经是偏向锁了，但是里面存储的线程ID并不是自己
 *    （是第一个线程的ID），那么它会进行CAS（Compare And Swap）,从而获取锁，这里存在2种情况：
 *    1.获取锁成功：那么它会直接将Mark Word中的线程ID由第一个线程变成自己（偏向锁标记保持不变），这样该对象依然会保持偏向锁状态
 *    2.获取锁失败：则表示这时可能会有多个线程同时在尝试争抢该对象的锁，那么这时偏向锁就会进行升级，升级为轻量级锁
 *
 *    自旋锁：
 *    若自旋失败（依然无法获取到锁），那么锁就会转化为重量级锁，在这种情况下，无法获取到锁的线程都会进入到Monitor（即内核态）
 *    自旋最大的一个特点就是避免了线程从用户态进入到内核态
 *
 *    重量级锁：
 *    线程最终从用户态进入到内核态，操作系统来进行线程同步控制
 *
 */
public class Test03 {
	public static void main(String[] args) {

		Job job1 = new Job();
		Job job2 = new Job();

		T1 t1 = new T1(job1);
		T2 t2 = new T2(job2);

		t1.start();
		t2.start();

	}

	static class Job {
		public synchronized void hello() {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ConsolePrinter.printlnCyan("hello");
		}

		public static synchronized void world() {
			ConsolePrinter.printlnCyan("world");
		}

		public void say() {
			synchronized (this) {
				ConsolePrinter.printlnCyan("hello world!");
			}
		}
	}

	static class T1 extends Thread {
		private Job job;
		public T1(Job job) {
			this.job = job;
		}

		@Override
		public void run() {
			job.hello();
		}
	}

	static class T2 extends Thread {
		private Job job;
		public T2(Job job) {
			this.job = job;
		}

		@Override
		public void run() {
			job.world();
		}
	}

}
