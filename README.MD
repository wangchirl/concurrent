## 个人笔记整理 - Shadow
  
### Thread

    1、Thread类的重要方法
     ①、sleep()：睡眠一段时间
     ②、yield()：让出cpu执行时间
     ③、join()：等待另外一个线程结束
     ④、start()：启动线程
     ⑤、run()：真正执行的方法
    
    2、线程状态 Thread.State
     ①、NEW ：线程新建状态 new Thread()
     ②、RUNNABLE：可执行状态，包括READY和RUNNING，start()后
       * READY：线程被挂起（CPU时间片耗完）、Thread.yield()
       * RUNNING：线程调度器选中执行
     ③、BLOCKED：阻塞状态，等待进入同步代码块的锁 
     ④、WAITING：等待状态，o.wait()、t.join()、LockSupport.park()；通过o.notify()、o.notifyAll()、LockSupport.unpark()进入可执行状态
     ⑤、TIMED_WAITING：超时等待状态，Thread.sleep(time)、o.wait(time)、t.join(time)、LockSupport.parkNanos()、LockSupport.parkUntil()；等待时间结束进入可执行状态
     ⑥、TERMINATED：线程销毁状态

   ![线程状态](./线程状态.png)
   
   
    3、启动线程的方式
     ①、new Thread() 方式
     ②、new Runnable() 方式
     ③、线程池ThreadPool方式
   
   
  
### synchronized 关键字
   
   [关于C++代码Monitor对象加锁的底层实现](http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file/4ad2d6087d73/src/share/vm/runtime/objectMonitor.cpp)
  
     1、synchronized 的使用
      修饰成员方法 => 锁的是当前对象
      修饰静态方法 => 锁的是当前对象的class对象
    
     2、字节码层面：
      ACC_SYNCHRONIZED 方法（成员方法或静态方法）
      monitorenter
      monitorexit
     
     3、修饰代码块
      monitorenter
      monitorexit
      一个 monitorenter 对应 monitorexit 可能是 1:1（抛异常情况）或1:2（一般情况）
     
     4、修饰方法（成员或静态方法）
      ACC_SYNCHRONIZED 访问修饰符来控制
     
     5、JVM中的同步是基于进入与退出监视器对象（管程对象）（Monitor）来实现的
     每个对象实例都会有一个Monitor对象，Monitor对象会和Java对象一同创建
     与销毁，Monitor对象是由C++来实现的（OpenJDK）
      
     当多个线程同时访问一段同步代码时，这些线程会被放到一个EntryList集合中，
     处于阻塞状态的线程都会被放到该列表中，接下来，当线程获取到对象的Monitor时，
     Monitor是依赖底层操作系统的mutex lock来实现互斥的，线程获取mutex成功，
     则会持有mutex，这时其他线程就无法再获取到该mutex
 
     如果线程调用了wait方法，那么该线程就会释放掉所持有的mutex，并且该线程会进入
     到WaitSet集合（等待集合）中，等待下一次被其他线程调用notify/notifyAll唤醒，
     如果当前线程顺利执行完毕方法，那么它就会释放掉所持有的mutex
     
     总结：
     同步锁在这种实现方式中，业务Monitor是依赖底层操作系统实现，这样就会存在用户态和
     内核态之间的切换，所以会增加性能开销
 
     通过对象互斥锁的概念来保证共享数据操作的完整性，每个对象都对应于一个可称为【互斥锁】
     的标记，这个标记用于保证在任何时刻只能有一个线程访问该对象     
    
     那些处于EntryList与WaitSet中的线程均处于阻塞状态，阻塞操作是由操作系统来完成的，
     在linux下是通过 pthread_mutex_lock函数来完成的，线程被阻塞后便会进入到内核调度状态
     这会导致系统在用户态与内核态之间来回切换，严重影响锁的性能
 
     解决上述问题的办法便是自旋，其原理是：当发生对Monitor的争用时，若Owner能够在很短的时间内
     释放掉锁，则那些争用的线程就可以稍微等待一下（即所谓的自旋），在Owner线程释放锁之后，争用
     线程可能会立刻获取到锁，从而避免了系统阻塞。不过，当Owner运行的时间超过了临界值后，争用线程
     自旋一段时间后依然无法获取到锁，这时争用线程则会停止自旋而进入到阻塞状态。所以总体的思想是：
     不成功再进行阻塞，尽量降低阻塞的可能性，这对那些执行时间很短的代码块来说有极大的性能提升，
     显然，自旋在多处理器（多核心）上才有意义
     
	  互斥锁的属性：
	  1、PTHREAD_MUTEX_TIMED_NP：这是缺省值，也就是普通锁。当一个线程加锁以后，其他请求锁的线程将
	     会形成一个等待队列，并且在解锁后按照优先级获取到锁，这种策略可以确保资源分配的公平性
	  2、PTHREAD_MUTEX_RECURSIVE_NP：嵌套锁。允许一个线程对同一个锁成功获取多次，并通过unlock解锁
	     如果是不同线程请求，则在加锁线程解锁时重新进行竞争
	  3、PTHREAD_MUTEX_ERRORCHECK_NP：检错锁。如果一个线程请求同一个锁，则返回ENEADLK，否则与
	     PTHREAD_MUTEX_TIMED_NP类型动作相同，这样就保证了当不允许多次加锁时不会出现最简单情况下的死锁
	  4、PTHREAD_MUTEX_ADAPTIVE_NP：适应锁，动作最简单的锁类型，仅仅等待解锁后重新竞争
	  

注意：<font color="red">**出现异常会导致锁的释放**</font>

### synchronized 锁升级
 
  	  在JDK1.5之前，我们若想实现线程同步，只能通过 synchronized 关键字这一种方式来达成，在底层，Java也是通过
  	  synchronized 关键字来做到数据的原子性维护的；synchronized 关键字是JVM实现的一种内置锁，从底层角度来说，
  	  这种锁的获取与释放都是由JVM帮助我们隐式实现的
 
  	  从JDK1.5开始，并发包引入了 Lock 锁，Lock 同步锁是基于Java来实现的，因此锁的获取与释放都是通过Java代码
  	  来实现与控制的；然而，synchronized 是基于底层操作系统的 Mutex Lock 来实现的，每次对锁的获取与释放动作
  	  都会带来用户态与内核态之间的切换，这种切换会极大的增加系统的负担，在并发量较高时，也就是说锁的竞争比较激
  	  烈时，synchronized 锁在性能上的表现就非常差
 
  	  从JDK1.6开始，synchronized 锁的实现发生了比较大的变化，JVM 引入了相应的优化手段来提升 synchronized 锁
  	  的性能，这种提升涉及到偏向锁、轻量级锁及重量级锁等，从而减少锁的竞争锁带来的用户态与内核态之间的切换；这种
  	  锁的优化实际上是通过Java对象头中的一些标志位来去实现的；对于锁的访问与改变，实际上都是与Java对象头息息相关
 
     从JDK1.6开始，对象实例在堆当中会被划分为三个组成部分：对象头、实例数据与对齐填充
 
     对象头主要由三块内容来构成：
     1.Mark Word
     2.指向类的指针
     3.数组长度（仅限数组对象）
 
     其中Mark Word（它记录了对象、锁及垃圾回收相关的信息，在64位JVM中，其长度也是64bit）的位信息包括了如下组成部分：
     1.无锁标记
     2.偏向锁标记
     3.轻量级锁标记
     4.重量级锁标记
     5.GC标记
 
     对于 synchronized 锁来说，锁的升级主要都是通过 Mark Word中的锁标志位与是否是偏向锁标志位来达成的，synchronized
     关键字所对应的锁都是先从偏向锁开始，随着锁竞争的不断升级，逐步演化至轻量级锁，最后则变成了重量级锁
 
     对于锁的演化来说，它会经历如下阶段：
     无锁 -> 偏向锁 -> 轻量级锁（自旋是轻量级锁中的一种） -> 重量级锁
 
     偏向锁：
     针对于一个线程来说的，它的主要作用就是优化同一个线程多次获取一个锁的情况；如果一个 synchronized 方法被一个线程访问
     那么这个方法所在的对象就会在其 Mark Word中进行偏向锁的标记，同时还会有一个字段来存储该线程的ID；当这个线程再次访问
     同一个 synchronized 方法时，它会检查这个对象的 Mark Word的偏向锁标记以及是否指向了其线程ID，如果是的话，那么该线程
     就无需再去进入管程（Monitor）了，而是直接进入到该方法体中。
     如果是另外一个线程访问这个 synchronized 方法，那么偏向锁会被取消掉
 
     轻量级锁：
     若一个线程已经获取到了当前对象的锁，这时第二个线程又开始尝试争抢该对象的锁，由于该对象的锁已经被第一个线程获取到，因此
     它是偏向锁，而第二个线程在争抢时，会发现该对象头中的Mark Word 已经是偏向锁了，但是里面存储的线程ID并不是自己
     （是第一个线程的ID），那么它会进行CAS（Compare And Swap）,从而获取锁，这里存在2种情况：
     1.获取锁成功：那么它会直接将Mark Word中的线程ID由第一个线程变成自己（偏向锁标记保持不变），这样该对象依然会保持偏向锁状态
     2.获取锁失败：则表示这时可能会有多个线程同时在尝试争抢该对象的锁，那么这时偏向锁就会进行升级，升级为轻量级锁
 
     自旋锁：
     若自旋失败（依然无法获取到锁），那么锁就会转化为重量级锁，在这种情况下，无法获取到锁的线程都会进入到Monitor（即内核态）
     自旋最大的一个特点就是避免了线程从用户态进入到内核态
 
     重量级锁：（自旋10次以后升级为重量级锁 - OS）
     线程最终从用户态进入到内核态，操作系统来进行线程同步控制	   

### 自旋还是系统锁？
    1、执行时间短（加锁代码），线程数少，用自旋
    2、执行时间长，线程数多，用系统锁     
     
     
### JIT编译器对锁的优化    
    1、锁消除
     JIT 编译器（Just In Time编译器）可以在动态编译同步代码时，使用一种叫做逃逸分析的技术，来通过该项技术判别程序中锁使用
     的锁对象是否只被一个线程锁使用，而没有散布到其他线程当中；如果情况就是这样的话，那么JIT编译器在编译这个同步代码时就不会
     生成 synchronized 关键字锁标识的锁的申请与释放机器码，从而消除了锁的使用流程     
    2、锁粗化
     JIT编译器在执行动态编译时，若发现前后相邻的 synchronized 块使用的是同一个锁对象，那么它会把这几个 synchronized 块
     给合并为一个较大的同步块，这样做的好处在于线程在执行这些代码时，就无需频繁申请与释放锁了，从而达到申请与释放锁一次，
     就可以执行完全部的同步代码块，从而提升了性能   
     对应的锁细化：尽可能的减少synchronized块的代码量
     

### 死锁、活锁、饿死
    1、死锁：线程1等待线程2互斥持有的资源，而线程2也在等待线程1互斥持有的资源，两个线程都无法继续执行
    2、活锁：线程持续重试一个总是失败的操作，导致无法继续执行
    3、饿死：线程一直被调度器延迟访问其赖以执行的资源，也许是调度器先于低优先级的线程而执行高优先级的线程，同时总是会有一个
            高优先级的线程可以执行，饿死也叫做无限延迟
     
     
    检测线程死锁的方法：
    1、可视化工具
      ① jvisualvm
      ② jconsole
      ③ jmc
     2、命令行
       jps -l ：找到 java程序进程号
       jstack pid : 查看线程栈信息

#### 总结：
    1、synchronized(Object)
       - 不能用String常量，Integer，Long
       - 锁的是对象不是代码
       - 代码块使用对象加锁时最好限定对象 final 修饰
       - 成员方法锁的是当前对象 this
       - 具体方法锁的是当前类Class对象
       - 锁升级：偏向锁-自旋锁（轻量级锁）- 重量级锁
       - 线程数少-自旋；多-重量级锁
       - 操作耗时长-重量级锁
   
    2、volatile 
       - 保证线程可见性：MESI、缓存一致性协议
       - 禁止指令重排序（读写屏障：loadfence、storefence原语指令）
       
          
    
    
### volatile 关键字
    
     volatile关键字主要有三方面的作用：
      1、实现 long/double 类型变量的原子操作
      2、防止指令重排序
      3、实现变量的可见性
    
      当使用 volatile关键字修饰变量时，应用就不会从寄存器中获取该变量的值，而是从内存（高速缓存）中获取
    
      volatile与锁类似的地方有两点：
       1、确保变量的内存可见性
       2、防止指令重排序
    
      volatile可以确保对变量写操作的原子性，但不具备排他性
      另外的重要一点在于：使用锁可能会导致线程的上下文切换（内核态与用户态之间的切换），但使用volatile并不会出现这种情况
    
      不能保证原子操作
      volatile int a = b + 1;
      volatile int a = a++;
      一般这样用
      volatile int count = 1;
      volatile boolean flag = false;
    
      如果要实现volatile写操作的原子性，那么在等号右侧的赋值变量中就不能出现被多线程所共享的变量，哪怕这个变量也是被volatile修饰
    
      volatile Date date = new Date();
    
      防止指令重排序与实现变量的可见性都是通过一种手段来实现的：内存屏障（memory barrier）
    
      int a = 1;
      String s = "hello";
    
      内存屏障（Release Barrier，释放屏障）
      volatile boolean v = false; // 写操作
      内存屏障（Store Barrier，存储屏障）
    
      Release Barrier：防止下面的volatile与上面的所有操作的指令重排序
      Store Barrier：重要作用是刷新处理器缓存，结果是可以确保该存储屏障之前一切的操作所生成的结果对于其他处理器来说都可见
    
      内存屏障（Load Barrier，加载屏障）
      boolean v1 = v; //读操作
      内存屏障（Acquier Barrier，获取屏障）
      int a = 1;
      String s = "world";
    
      Load Barrier：可以刷新处理器缓存，同步其他处理器对该volatile遍历的修改结果
      Acquire Barrier：可以防止上面的volatile读取操作与下面的所有操作语句的指令重排序
    
      对于volatile关键字冰冷的读写操作，本质上都是通过内存屏障来执行的
      内存配置兼具了两方面的能力：1、防止指令重排序；2、实现变量内存的可见性
    
      1、对于读取操作来说，volatile可以确保该操作与其后续的所有读写操作都不会进行指令重排序
      2、对于修改操作拉屎，volatile可以确保该操作与其上面的所有读写操作都不会进行指令重排序
    
     
      volatile与锁的一些比较：
       锁同样具备变量内存可见性与防止指令重排序的功能
       
       monitorenter
       内存屏障（Acquier Barrier，获取屏障）
       ...
       内存屏障（Release Barrier，释放屏障）
       monitorexit   
    
      volatile常见场景：
       1、DCL（双重检查）单例 - 为了禁止指令重排序（CPU）
       2、JUC并发包

    
### Java内存模型(JMM)以及 happens-before [Java规范关于JMM的说明](https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4)  
       1、变量的原子性问题 
       2、变量的可见性问题
       3、变量修改的时序性问题
    
       happens-before重要规则：
       1、顺序执行规则（限定在单个线程上的）：该线程的每个动作都happen-before它的后面的动作
       2、隐式锁（monitor）规则：一个线程的 unlock happen-before 另外一个线程的 lock，之前的线程对于同步代码块的所有执行结果对于后续获取锁的线程来说都是可见的
       3、volatile读写规则：对于一个volatile变量的写操作一定会 happen-before后续对该变量的读操作
       4、多线程的启动规则：Thread对象的start方法happen-before该线程run方法中的任何一个动作，包括在其中启动的任何子线程
       5、多线程的终止规则：一个线程启动一个子线程，并且调用了子线程的join方法等待其结束，那么当子线程结束后，父线程的接下来的所有操作都可以看到子线程run方法中的执行结果
       6、线程的中断规则：可以调用interrupt方法来中断线程，这个调用happen-before对该线程中断的检查（isInterrupted）
        
    
    
### synchronized与volatile
    1、synchronized     
     ①、保证线程可见性
     ②、保证有序性（无法禁止指令重排序，由于同一时刻只能有一个线程执行，存在as-if-serial语义，因此保证有序性）
     ③、保证原子性
    2、volatile
     ①、保证线程可见性
     ②、禁止指令重排序
     ③、无法保证原子性
 
 
### 问题：为什么synchronized无法禁止指令重排序，却能保证有序性？
    1、为了进一步提升计算机各方面能力，在硬件层面做了很多优化，如处理器优化和指令重排等，但是这些技术的引入就会导致有序性问题
    2、最好的解决有序性问题的办法，就是禁止处理器优化和指令重排，就像volatile中使用内存屏障一样（读写屏障是为了禁止指令重排序）
    3、在Java中，不管怎么排序，都不能影响单线程程序的执行结果。这就是as-if-serial语义，所有硬件优化的前提都是必须遵守as-if-serial语义
    4、synchronized，他是Java提供的锁，可以通过他对Java中的对象加锁，并且他是一种排他的、可重入的锁
    5、synchronized通过排他锁的方式就保证了同一时间内，被synchronized修饰的代码是单线程执行的。所以呢，这就满足了as-if-serial语义的一个关键前提，那就是单线程，因为有as-if-serial语义保证，单线程的有序性就天然存在了    
    
  


     
### Lock、Condition、AQS同步器     
   
     1、关于 Lock 与 synchronized 关键字在锁的处理上的重要差别：
        ① 锁的获取方式：前者是通过程序代码的方式由开发者手工获取，后者是通过JVM来获取（无需开发者干预）
        ② 具体实现方式：前者是通过Java代码的方式来实现，后者是通过JVM底层来实现（无需开发者关注）
        ③ 锁的释放方式：前者务必通过unlock()方法在finally块中手工释放锁，后者是通过JVM来释放（无需开发者关注）
        ④ 锁的具体类型：前者提供了多种锁，如公平锁、非公平锁，后者是非公平锁；二者都提供了可重入锁
     
     
     2、Condition -> await()、signal()、signalAll() + Lock 类 [multiple wait sets]
        Object    -> wait()、notify()、notifyAll() + synchronized 关键字 [waitSet + EntryList]
       ①传统上，我可以通过synchronized关键字 + wait、notify、notifyAll 来实现多个线程之间的协调与通信，
        整个过程都是JVM来帮助我们实现的，开发者无需（也无法）了解底层的实现细节
       ②从JDK5开始，并发包提供了Lock、Condition（await、signal、signalAll）来实现多个线程之间的协调与通信，
        整个过程都是由开发者来控制的，而且相比于传统方式，更加灵活，功能也更加强大
        
       Condition的方法：
        1、await()：线程进入等待状态，并释放与之关联的Lock，直到被signalled或interrupted
           四种情况被唤醒：
           ① 别的线程调用了同一个 Condition 的 signal()方法且当前线程被选中为被唤醒的线程时
           ② 别的线程调用同一个 Condition 的 signalAll()方法
           ③ 别的线程 interrupts 中断当前线程且支持线程挂起中断
           ④ 虚假唤醒发生
           在所有情况下，在此方法可以返回当前线程之前必须重新获取与此 Condition 关联的锁
        2、signal()：**唤醒一个等待的线程**，如果存在多个线程在 Condition 等待，则选择其中的一个进行唤醒，在 wait 返回前线程必须获取到关联的 Lock
        3、signalAll()：**唤醒所有等待的线程** ，在 wait 返回前每个线程必须重新获取关联的 Lock       

      **Condition 的使用自行查阅 ArrayBlockingQueue 的实现**
    
     3、Thread.sleep 与 await（或者是 Object的wait方法）的本质区别：
        sleep方法本质上不会释放锁，而await会释放锁，并且在 signal 后，还需要重新获得锁才能继续执行（该行为与Object的wait方法完全一致）    
    
     4、综合运用 => Test02  
  
  

### CAS 、原子类 Atomicxxx、原子引用 AtomicReference/AtomicStampReference
    1、CAS ==> Test07
     ①、synchronized关键字与Lock等锁机制都是悲观锁：无论做任何操作，首先都需要先上锁，接下来再去执行后续操作，从而确保了
       接下来的所有操作都是由当前这个线程来执行的
     ②、乐观锁：线程在操作之前不会做任何预先处理，而是直接去执行；当在最后执行变量更新的时候，当前线程需要有一种机制来确保
       当前被操作的变量是没有被其他线程修改的；CAS是乐观锁的一种极为重要的实现方式
  
     CAS（Compare And Swap/Set）
     比较与交换：这是一个不断循环的过程，一直到变量值被修改成功为止；CAS本身是由硬件指令来提供支持的，换句话说，硬件中是通过
     一个原子指令来实现比较与交换的；因此，CAS可以确保变量操作的原子性
  
  
     对于CAS来说，其操作数主要涉及到如下三个：
      1.需要被操作的内存值V（主内存值）
      2.需要进行比较的值A（期望的值，即当前线程读取到的值）
      3.需要进行写入的值B（更新值）
     只有当V==A的时候，CAS才会通过原子操作的手段来将V的值更新成功，并且返回旧值
   
     关于CAS的限制或问题：
      1.循环开销问题：并发量大的情况下会导致线程一直自旋
      2.只能保证一个变量的原子操作：可以通过AtomicReference来实现对多个变量的原子操作
      3.ABA问题：1 -> 2 -> 1，经过修改之后再改回来的问题
      
      ABA问题的解决办法：使用带版本号的原子引用 AtomicStampedReference
       

### 关于 synchronized、Atomic原子类、LongAdder的性能测试
```java
public class Test01 {
	final Object lock = new Object();
	static long count1 = 0L;
	static AtomicLong count2 = new AtomicLong(0L);
	static LongAdder count3 = new LongAdder();

	public static void main(String[] args) throws InterruptedException {
		Thread[] threads = new Thread[1000];
		// synchronized
		Test01 test01 = new Test01();
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() ->{
				for (int j = 0; j < 100000; j++) {
					test01.increment();
				}
			});
		}
		long start = System.currentTimeMillis();
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		System.out.println("值为："+ count1 +" sync 耗时：" + (System.currentTimeMillis() - start));

		// atomic
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() ->{
				for (int j = 0; j < 100000; j++) {
					count2.getAndIncrement();
				}
			});
		}
		start = System.currentTimeMillis();
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		System.out.println("值为："+ count2.get() +" atomic 耗时：" + (System.currentTimeMillis() - start));

		// longAdder
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() ->{
				for (int j = 0; j < 100000; j++) {
					count3.increment();
				}
			});
		}
		start = System.currentTimeMillis();
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		System.out.println("值为："+ count3.longValue() +" longAdder 耗时：" + (System.currentTimeMillis() - start));
	}

	public void increment() {
		synchronized (lock){
			count1++;
		}
	}
}
```



### ReentrantLock、ReentrantReadWriteLock
    1、ReentrantLock 方法
     ① lock() ：加锁
     ② unlock() ：释放锁
     ③ tryLock() ：尝试加锁
     ④ lockInterruptibly() ：可中断的加锁
     ⑤ ReentrantLock(fair) ： 构造方法，可选择公平与非公平
    2、ReentrantReadWriteLock 方法
     ① readLock() ： 读锁
     ② writeLock() ：写锁
     ③ ReentrantReadWriteLock(fair) ：构造方法，可选择公平与非公平


### CountDownLatch、CyclicBarrier、Phaser、Semaphore、Exchanger
  
    1、CountDownLatch => Test04
     允许一个或多个线程等待在其他线程中执行的一组操作完成
     ① countDown()：调用此方法需在 finally 块中，子任务每执行完一次调用一次
     ② await()：主线程等待，直到维护的state变量值变为0，可多次等待，如果state已经是0则会立即执行 private volatile int state
     ③ await(time)：超时等待，时间过来住线程立即执行，不会等待子线程全部执行完毕
     ④ CountDownLatch 不可重用
     
    
    2、CyclicBarrier => Test05
     栅栏，多个任务全部到达后（最后一个线程到达）再继续执行，不存在子任务的概念
     ① await()：线程等待所有的任务到达屏障
     ② await(time)：超时等待，会抛异常
     ③ CyclicBarrier(int,Runnable) ：构造方法
     > 底层执行逻辑：
      1.初始化CyclicBarrier中的各种成员变量，包括parties、count和Runnable（可选）
      2.当调用await方法时，底层会先检查计数器是否已经归零，如果是的话，那么就首先执行可选的Runnable，接下来开始下一个Generation
      3.在下一个分代中，将会重置count值为parties，并且创建新的Generation实例
      4.同时会调用Condition的signalAll方法，唤醒所有的在屏障前面等待的线程，让其开始继续执行
      5.如果计数器没有归零，那么当前的调用线程将会通过Condition的await方法，在屏障前进行等待
      6.以上所有执行流程均在Lock锁的控制范围内，不会出现并发问题（--count）
  
     > 更好的对 CountDownLatch 与 CyclicBarrier 的理解：==> Test05
        CyclicBarrier：赛跑前先来的选手需要等待其他选手入场，然后进行比赛，栅栏可重用
        CountDownLatch：需要等待所有赛跑选手到达目的地后才可结束比赛，闭锁不可重用

     3、Semaphore ==> Test08
       信号量通常用于限制线程的数量访问一些(物理或逻辑)资源。举个例子一个使用信号量来控制访问资源的线程数量
       ① acquire() ：获取锁，阻塞方法
       ② release() : 释放锁
       ③ Semaphore(int) ：构造方法，指定限定数
       ③ Semaphore(int, fair) ：构造方法，指定限定数及是否公平
       
      > 限流 => 类比高速收费站
     
     4、Phaser => Test09
       多个栅栏，可看做是CountDownLatch和CyclicBarrier的结合
       ① bulkRegister(int) ：规定桶的数量
       ② arriveAndAwaitAdvance() ：继续参与下一个栅栏
       ③ arriveAndDeregister() ：不在参与下一个栅栏
       ④ onAdvance() ： 栅栏被推倒的时候自动回调的方法
      
     5、Exchanger => Test10
      ① exchange(r)：交换
      ② exchange(r,t,u)：超时交换
      > 只能是2个线程间的数据交互；如游戏中2个人交换装备
      

### ReentrantLock（Lock） 与 ReentrantReadWriteLock（ReadWriteLock）
    二者性能测试 => Test11
    1、ReentrantLock
     ① lock()
     ② unlock()
     
     对于ReentrantLock来说，其执行逻辑如下所示：
     1. 尝试获取对象的锁，如果获取不到（意味着已经有其他线程持有了锁，并且尚未释放），那么它就会进入到AQS的阻塞队列中。
     2. 如果获取到，那么根据所是个公平锁还是非公平屁锁来进行不同的处理：
       ① 如果是公平锁，那么线程会直接放置到AQS阻塞队列的末尾
       ② 如果是非公平锁，那么线程会首先尝试进行CAS计算，如果成功，则直接获取到锁
         如果失败，则与公平锁的处理方式一致，被放到阻塞队列末尾
     3. 当锁释放时（调用unlock方法），那么底层会调用release方法对state成员变量值进行减一操作，如果减一后，state值不为0
        那么release操作就执行完毕；如果减一操作后，state值为0，则调用LockSupport的unpark方法唤醒该线程后的等待队列中的
        第一个后续线程，将其唤醒，使之能够获取到对象的锁（release时，对于公平锁与非公平锁的处理逻辑是一致的）；之所以调用
        release方法后state值可能不为零，原因在于ReentrantLock是可重入锁，表示线程可以调用多次lock方法，导致每调用一次
        state的值都会加一
      
      对于ReentrantLock来说，所谓的上锁，本质上就是对AQS中的state成员变量的操作：对state+1表示上锁；对state-1表示释放锁
        
     
    2、ReentrantReadWriteLock
     ① readLock().lock()
     ② readLock().unlock()
     ③ writeLock().lock()      
     ④ writeLock().unlock()      
    > 读锁共享锁、写锁互斥锁
    
     关于ReentrantReadWriteLock的执行逻辑：
     1. 读锁：
       ① 在获取读锁时，会尝试判断当前对象是否拥有了写锁，如果已经拥有，则直接失败
       ② 如果没有写锁，就表示当前对象没有排它锁，则当前线程会尝试给对象加锁
       ③ 如果当前线程已经持有了搞对象的锁，那么直接将读锁数量+1
       
     2. 写锁：
       ① 在获取写锁时，会尝试判断当前对象是否拥有了锁（读锁与写锁），如果已经拥有且持有的线程并非当前线程，直接失败
       ② 如果当前对象没有被加锁，那么写锁就会为当前对象上锁，并且将写锁的个数+1
       ③ 将当前对象的排它锁线程持有者设为自己
       
       

### ReentrantLock 与 synchronized 比较
    1、CAS 与 synchronized锁升级
    2、tryLock 尝试获取锁
    3、lockInterruptibly 可被中断
    4、ReentrantLock可实现公平与非公平


### <font color="blue">AQS（AbstractQueuedSynchronizer）抽象队列同步器</font>
    
    1、ConditionObject impl Condition ，其中每一个Condition都维护了一个等待队列（类似synchronized中的 waitSet）=> await()、signal()、signalAll()
    2、Node 双向阻塞队列FIFO（类似synchronized中的EntryList），持有线程对象
    3、private volatile int state; 同步状态，在不同的实现中作用不一样
      ① ReentrantLock - 加锁状态，为 0 时则表示未加锁状态，大于0则表示加锁状态（可重入锁）
      ② ReentrantReadWriteLock - 分为前高16位来表示读的状态和低16位来表示写的状态
      ③ CountDownLatch - 需要 countDown的次数
      ④ Semaphore - 信号量大小
    
   ![AQS](./AQS.png)

   [关于LockSupport的park、unpark方法的底层实现](http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file/4ad2d6087d73/src/os/linux/vm/os_linux.cpp)
    
### <font color="red">重点：关于AQS与synchronized关键字之间的关系 - 对照理解</font>

    1、synchronized：
     ①、synchronized关键字在底层的C++实现中，存在两个重要的数据结构（集合）：WaitSet、EntryList
     ②、WaitSet中存放的是调用了Object的wait方法的线程对象（被封装成了C++的Node对象）    
     ③、EntryList中存放的是陷入到阻塞状态、需要获取monitor的那些线程对象
     ④、当一个线程被notify后，它就会从WaitSet中移动到EntryList中
     ⑤、进入到EntryList后，该线程依然需要与其他线程争抢monitor对象
     ⑥、如果争抢到，就表示该线程获取到了对象的锁，它就可以以排他方式执行对应的同步代码
    
    2、AQS：
     ①、AQS中存在两种队列，分别是Condition对象上的条件队列wait queue，以及AQS本身的阻塞队列sync queue
     ②、这两个队列中的每一个对象都是Node实例（里面封装了线程对象）
     ③、当位于Condition条件队列中的线程被其他线程signal后，该线程就会从条件队列中移动到AQS的阻塞队列中
     ④、位于AQS阻塞队列中的Node对象本质上都是由一个双向链表来构成的
     ⑤、在获取AQS锁时，这些进入到阻塞队列中的线程会按照在队列中的排序先后尝试获取锁
     ⑥、当AQS阻塞队列中的线程获取到锁后，就表示该线程已经可以正常执行了
     ⑦、陷入到阻塞状态的线程，依然需要进入到操作系统的内核态，进入阻塞（park方法实现）
    
    



### 线程池 ThreadPoolExecutor








         