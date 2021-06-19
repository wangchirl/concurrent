## 1、线程池

- Executor
- ExecutorService
- AbstractExecutorService
  - ThreadPoolExecutor
  - ForkJoinPool
- ScheduleExecutorService
  - ScheduleThreadPoolExecutor
- Executors



![1624024421050](G:\JDK源码\ThreadPoolExecutor.assets\1624024421050.png)



### Executor

```java
public interface Executor {
    // 执行任务
    void execute(Runnable command);
}
```

### ExecutorSerivce

> 对父接口进行了功能扩展
>
> - 控制线程池的操作
> - 提交任务的操作
> - 支持有返回值的任务
> - 执行多个和任意任务的操作

```java
public interface ExecutorService extends Executor {
    // 关闭线程池
    void shutdown();
    // 立即关闭线程池
    List<Runnable> shutdownNow();
    // 线程池是否已关闭
    boolean isShutdown();
    // 线程池是否已销毁
    boolean isTerminated();
    // 等待线程池销毁 timout + unit
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;
    // 提交任务的方法 Callable 有返回值的任务
    <T> Future<T> submit(Callable<T> task);
    // 提交任务的方法 Runnable + result 也支持返回值
    <T> Future<T> submit(Runnable task, T result);
    // 提交 Runable 无返回值
    Future<?> submit(Runnable task);
    // 执行所有的任务
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;
    // 执行所有的任务 + 超时时间限制
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;
    // 执行任意一个任务
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;
    // 执行任意一个任务 + 超时时间限制
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```



### AbstractExecutorService

> 实现基本的方法
>
> - 提供模板方法
> - 具体的执行方法逻辑

1. ##### 创建任务  RunnableFuture 任务 -> FutureTask

   ```java
   // 接受 Runnable 任务 + T 的返回值类型
   protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
       return new FutureTask<T>(runnable, value);
   }
   // 接受 Callable 任务（自带返回值）
   protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
       return new FutureTask<T>(callable);
   }
   ```

2. ##### 提交任务

   ```java
   // 提交 Runnable 任务
   public Future<?> submit(Runnable task) {
       // 基本判空
       if (task == null) throw new NullPointerException();
       // 创建任务
       RunnableFuture<Void> ftask = newTaskFor(task, null);
       // 执行任务
       execute(ftask);
       // 返回结果
       return ftask;
   }
   // 提交 Runnable + T 返回值的任务
   public <T> Future<T> submit(Runnable task, T result) {
       // 基本判空
       if (task == null) throw new NullPointerException();
       // 创建任务
       RunnableFuture<T> ftask = newTaskFor(task, result);
       // 执行任务
       execute(ftask);
       // 返回结果
       return ftask;
   }
   // 提交 Callable 任务
   public <T> Future<T> submit(Callable<T> task) {
       // 基本判空
       if (task == null) throw new NullPointerException();
       // 创建任务
       RunnableFuture<T> ftask = newTaskFor(task);
       // 执行任务
       execute(ftask);
       // 返回结果
       return ftask;
   }
   ```

3. ##### 执行任务 - 执行全部任务

   ```java
   // 不带超时时间的执行所有任务 - 一定要执行完全部，除非出现异常
   public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
       throws InterruptedException {
       // 基本判空
       if (tasks == null)
           throw new NullPointerException();
       ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
       // 标志位 - 是否全部完成
       boolean done = false;
       try {
           // 循环创建任务，并将任务执行，获取到异步执行的任务 futures
           for (Callable<T> t : tasks) {
               RunnableFuture<T> f = newTaskFor(t);
               futures.add(f);
               execute(f);
           }
           // 循环等待所有任务执行完成
           for (int i = 0, size = futures.size(); i < size; i++) {
               Future<T> f = futures.get(i);
               // 当前任务没有完成就调用 get 阻塞等待
               if (!f.isDone()) {
                   try {
                       f.get();
                   // 这里如果我们的任务出现异常，那么会抛出，程序将中断 -> 跳转到 finally 块
                   } catch (CancellationException ignore) {
                   } catch (ExecutionException ignore) {
                   }
               }
           }
           // 所有任务正常执行完成设置标志位 true，表示全部执行完成
           done = true;
           // 返回我们的结果
           return futures;
       } finally {
           // 如果没有被设置 true，表示任务没有完全被执行完，也即出现了异常情况
           if (!done)
               // 拿到任务，依次取消掉任务，当然已经完成的任务取消是没有效果的
               // 详情见 FutureTask
               for (int i = 0, size = futures.size(); i < size; i++)
                   futures.get(i).cancel(true);
       }
   }
   ```

4. ##### 执行任务 - 带超时时间执行全部任务

   ```java
   public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                        long timeout, TimeUnit unit)
       throws InterruptedException {
       // 基本判空
       if (tasks == null)
           throw new NullPointerException();
       // 时间转换为 纳秒 单位
       long nanos = unit.toNanos(timeout);
       // 异步执行的任务集合
       ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
       // 一样的全部任务完成的标准位
       boolean done = false;
       try {
           // 为了不影响计时出现偏差，这里先把所有的任务创建好
           for (Callable<T> t : tasks)
               futures.add(newTaskFor(t));
   		// 截至时间
           final long deadline = System.nanoTime() + nanos;
           final int size = futures.size();
   		// 执行任务
           for (int i = 0; i < size; i++) {
               execute((Runnable)futures.get(i));
               nanos = deadline - System.nanoTime();
               // 检查是否超时，超时直接返回，这里返回的任务就有可能没有执行的任务
               if (nanos <= 0L)
                   return futures;
           }
   		// 循环等待任务执行完成
           for (int i = 0; i < size; i++) {
               Future<T> f = futures.get(i);
               if (!f.isDone()) {
                   // 超时检查
                   if (nanos <= 0L)
                       return futures;
                   try {
                       f.get(nanos, TimeUnit.NANOSECONDS);
                   } catch (CancellationException ignore) {
                   } catch (ExecutionException ignore) {
                   } catch (TimeoutException toe) {
                       // 超时异常直接返回异步执行的任务 - 存在未完成的异步任务
                       return futures;
                   }
                   nanos = deadline - System.nanoTime();
               }
           }
           // 全部执行完成，返回异步执行的任务 - 全部都已经执行完成的
           done = true;
           return futures;
       } finally {
           // 同理，没有执行完成-出现了异常
           if (!done)
               // 依次取消任务 - 已完成的不会有影响
               // 详情见 FutureTask
               for (int i = 0, size = futures.size(); i < size; i++)
                   futures.get(i).cancel(true);
       }
   }
   ```

5. ##### 执行任务 - 执行任意一个任务

   ```java
   // 执行任意一个任务
   public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
       throws InterruptedException, ExecutionException {
       try {
           return doInvokeAny(tasks, false, 0);
       } catch (TimeoutException cannotHappen) {
           assert false;
           return null;
       }
   }
   // 带超时时间的执行任意一个任务
   public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                          long timeout, TimeUnit unit)
       throws InterruptedException, ExecutionException, TimeoutException {
       return doInvokeAny(tasks, true, unit.toNanos(timeout));
   }
   
   // 具体的执行方法
   private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks,
                             boolean timed, long nanos)
       throws InterruptedException, ExecutionException, TimeoutException {
       // 基本判空
       if (tasks == null)
           throw new NullPointerException();
       // 任务大小
       int ntasks = tasks.size();
       // 任务数为0的情况，抛异常
       if (ntasks == 0)
           throw new IllegalArgumentException();
       // 异步执行的任务集合
       ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
       // ExecutorCompletionService -> 将当前线程池 this 传入自身内部
       ExecutorCompletionService<T> ecs =
           new ExecutorCompletionService<T>(this);
       try {
           ExecutionException ee = null;
           // 超时时间
           final long deadline = timed ? System.nanoTime() + nanos : 0L;
           // 拿到任务迭代器
           Iterator<? extends Callable<T>> it = tasks.iterator();
   		// next() 拿到第一个任务丢给  ExecutorCompletionService，内部对任务进行二次封装
           futures.add(ecs.submit(it.next()));
           // 任务数量 -1
           --ntasks;
           // 正在执行的任务数量 +1
           int active = 1;
   		// 死循环
           for (;;) {
               // 任务执行完成时或任务取消时，会将任务丢到 ExecutorCompletionService 的队列中
               // 如果任务没有完成，那么 poll()方法返回的就是 null
               Future<T> f = ecs.poll();
               // 这里表示任务没有完成
               if (f == null) {
                   // 还有任务
                   if (ntasks > 0) {
                       // 任务数量 -1
                       --ntasks;
                       // 拿到下一个任务继续
                       futures.add(ecs.submit(it.next()));
                       // 正在执行的任务数量 +1
                       ++active;
                   }
                   // 如果正在执行的任务为 0 了，表示执行完了，执行 break退出循环
                   else if (active == 0)
                       break;
                   // 如果是带超时的情况，检查是否超时
                   else if (timed) {
                       f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                       if (f == null)
                           throw new TimeoutException();
                       nanos = deadline - System.nanoTime();
                   }
                   // 如果其他情况都不满足 - take 方法获取会抛异常
                   else
                       f = ecs.take();
               }
               // 如果有执行完成的任务
               if (f != null) {
                   // 正在执行的任务数量 -1
                   --active;
                   try {
                       // 拿到执行的结果返回
                       return f.get();
                   } catch (ExecutionException eex) {
                       ee = eex;
                   } catch (RuntimeException rex) {
                       ee = new ExecutionException(rex);
                   }
               }
           }
           if (ee == null)
               ee = new ExecutionException();
           throw ee;
       } finally {
           // 其他没有执行完成的任务，这里设置中断标志位
           for (int i = 0, size = futures.size(); i < size; i++)
               futures.get(i).cancel(true);
       }
   }
   ```



### ThreadPoolExecutor

1. HOW TO USE

   ```java
   public static void main(String[] args) throws InterruptedException {
   
       ThreadPoolExecutor executor = new ThreadPoolExecutor(2,
                                                            5, 
                                                            60, 
                                                            TimeUnit.SECONDS,
                                                            new LinkedBlockingQueue<>(3)
                                                           );
       // 提交 8 个任务
       for (int i = 0; i < 8; i++) {
           executor.submit(() -> {
               System.out.println(Thread.currentThread().getName());
           });
       }
       executor.shutdown();
       executor.awaitTermination(100,TimeUnit.SECONDS);
   }
   ```

   

2. ##### 原理图

   ![1624028074891](G:\JDK源码\ThreadPoolExecutor.assets\1624028074891.png)

3. ##### 推理

   - 线程容器 - 存放线程 maximumPoolSize
     - 核心线程 - corePoolSize
     - 临时线程 - （maximumPoolSize  - corePoolSize）
     - 临时线程存货时间 - long keepAliveTime 
     - 时间单位 - TimeUnit unit
   - 任务队列 - 存放任务 - BlockingQueue workQueue
   - 线程工厂 - 创建线程 - ThreadFactory threadFactory
   - 拒绝策略 - 任务满了的解决方案 - RejectedExecutionHandler handler

   ```java
   ThreadPoolExecutor(int corePoolSize,
                      int maximumPoolSize,
                      long keepAliveTime,
                      TimeUnit unit,
                      BlockingQueue<Runnable> workQueue,
                      ThreadFactory threadFactory,
                      RejectedExecutionHandler handler)
   ```

4. ##### 线程池工作原理

   - d第一次提交任务到线程池，创建核心线程处理任务，直到线程数量达到 corePoolSize 核心线程数
   - 持续提交任务到线程池，此时将任务存放到任务队列中 workQueue，直到任务队列满了
   - 继续提交任务，此时将创建临时线程任务处理，直到线程数量达到 maximumPoolSize 最大线程数
   - 此时==任务队列满了、线程数达到最大值了==如果继续提交任务，那么线程池将启用拒绝策略来拒绝任务，默认提供了四种拒绝策略
   - 随着时间的推移，任务越来越少，那么线程池将根据设置的 keepAliveTime 、unit 进行销毁线程，直到销毁的线程数量达到 corePoolSize 为止

5. ##### 线程池工厂

   ```java
   public interface ThreadFactory {
   	// 创建线程
       Thread newThread(Runnable r);
   }
   ```

6. ##### 四大拒绝策略 - RejectedExecutionHandler

   ```java
   // ① 直接抛异常
   public static class AbortPolicy implements RejectedExecutionHandler {
       public AbortPolicy() { }
       public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
           throw new RejectedExecutionException("Task " + r.toString() +
                                                " rejected from " +
                                                e.toString());
       }
   }
   // ② 调用者执行任务
   public static class CallerRunsPolicy implements RejectedExecutionHandler {
       public CallerRunsPolicy() { }
       public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
           if (!e.isShutdown()) {
               r.run();
           }
       }
   }
   // ③ 直接抛弃任务
   public static class DiscardPolicy implements RejectedExecutionHandler {
       public DiscardPolicy() { }
       public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
       }
   }
   // ④ 抛弃最早的任务
   public static class DiscardOldestPolicy implements RejectedExecutionHandler {
       public DiscardOldestPolicy() { }
       public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
           if (!e.isShutdown()) {
               // 任务队列队头任务弹出抛弃
               e.getQueue().poll();
               // 尝试执行新任务
               e.execute(r);
           }
       }
   }
   ```

7. ##### 阻塞队列 BlockingQueue - 多线程安全

   ```java
   // 增删改查
   public interface BlockingQueue<E> extends Queue<E> {
   	
       boolean add(E e);
   
       boolean offer(E e);
   
       void put(E e) throws InterruptedException;
   
       boolean offer(E e, long timeout, TimeUnit unit)
           throws InterruptedException;
   
       E take() throws InterruptedException;
   
       E poll(long timeout, TimeUnit unit)
           throws InterruptedException;
   
       int remainingCapacity();
   
       boolean remove(Object o);
   
   
       public boolean contains(Object o);
   
   
       int drainTo(Collection<? super E> c);
   
   
       int drainTo(Collection<? super E> c, int maxElements);
   }
   ```

   - **ArrayBlockingQueue**
   - **LinkedBlockingQueue**
   - **PriorityBlockingQueue**
   - SynchronousQueue

8. ##### FutureTask

   ```java
   public interface Future<V> {
   	// 取消任务
       boolean cancel(boolean mayInterruptIfRunning);
   	// 是否已取消
       boolean isCancelled();
   	// 是否完成
       boolean isDone();
   	// 获取结果
       V get() throws InterruptedException, ExecutionException;
   	// 超时获取结果
       V get(long timeout, TimeUnit unit)
           throws InterruptedException, ExecutionException, TimeoutException;
   }
   
   public interface RunnableFuture<V> extends Runnable, Future<V> {
       void run();
   }
   
   public class FutureTask<V> implements RunnableFuture<V> {
       
   }
   ```

9. #####  源码解读

   - 源码分析

     > - 向ThreadPool提交任务 - execute()
     > - 创建新线程 - addWorker(Runnable firstTask, boolean core)
     > - 线程的主循环 - Worker.runWorker(Worker w)
     > - 从队列中获取排队的任务 - getTask()
     > - 线程结束 - processWorkExit(Worker w, boolean completedAbruptyly)
     > - shutdown()、shutdownNow()、tryTerminated()

   - 变量

     ```java
     // ctl = 线程数量 + 线程池状态（高3位表示线程池状态，低29位表示线程池线程数量）
     private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
     private static final int COUNT_BITS = Integer.SIZE - 3;
     // 线程池线程容量 29个1
     private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
     
     // 线程池运行状态
     private static final int RUNNING    = -1 << COUNT_BITS; // 111 ctl < 0 时代表运行状态
     private static final int SHUTDOWN   =  0 << COUNT_BITS; // 000 
     private static final int STOP       =  1 << COUNT_BITS; // 001
     private static final int TIDYING    =  2 << COUNT_BITS; // 010 过度状态
     private static final int TERMINATED =  3 << COUNT_BITS; // 011
     // 获取当前线程池运行状态
     private static int runStateOf(int c)     { return c & ~CAPACITY; }
     // 获取当前线程池的线程数量
     private static int workerCountOf(int c)  { return c & CAPACITY; }
     // 获取 ctl 的值
     private static int ctlOf(int rs, int wc) { return rs | wc; }
     
     // 任务队列，存放任务
     private final BlockingQueue<Runnable> workQueue;
     // Worker -> Runnable workers 存放任务线程的集合
     private final HashSet<Worker> workers = new HashSet<Worker>();
     ```

     > ==ThreadPool 线程池的5种状态：==
     >
     > - **RUNNING** 	:接收新任务和进程队列任务
     > - **SHUTDOWN** :不接收新任务，但是接收进程队列任务
     > - **STOP**              :不接收新任务也不接收进程队列任务，并且打断正在执行中的任务
     > - **TIDYING**        :所有任务终止，待处理任务数量位0，线程池转为 TIDING，将会执行 terminated 钩子方法
     > - **TERMINATED**: terminated()执行完成
     >
     > ```java
     > RUNNING:  Accept new tasks and process queued tasks
     > SHUTDOWN: Don't accept new tasks, but process queued tasks
     > STOP:     Don't accept new tasks, don't process queued tasks
     >           and interrupt in-progress tasks
     > TIDYING:  All tasks have terminated, workerCount is zero,
     >           the thread transitioning to state TIDYING
     >           will run the terminated() hook method
     > TERMINATED: terminated() has completed
     > ```
     >
     > 状态之间的转换：
     >
     > - RUNNING -> SHUTDOWN：调用 shutdown() 方法
     > - (RUNNING/SHUTDOWN) -> STOP ：调用shutdownNow()方法
     > - SHUTDOWN -> TIDING ：队列和线程池都是空的
     > - STOP -> TIDING ：线程池为空
     > - TIDING -> TERMINATED ：钩子函数 terminated()执行完成

   - 构造器

     ```java
     // 构造器开始
     public ThreadPoolExecutor(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               BlockingQueue<Runnable> workQueue,
                               ThreadFactory threadFactory,
                               RejectedExecutionHandler handler) {
         // 基本判空
         if (corePoolSize < 0 ||
             maximumPoolSize <= 0 ||
             maximumPoolSize < corePoolSize ||
             keepAliveTime < 0)
             throw new IllegalArgumentException();
         if (workQueue == null || threadFactory == null || handler == null)
             throw new NullPointerException();
         this.acc = System.getSecurityManager() == null ?
             null :
         AccessController.getContext();
         // 基本变量赋值
         this.corePoolSize = corePoolSize;
         this.maximumPoolSize = maximumPoolSize;
         this.workQueue = workQueue;
         this.keepAliveTime = unit.toNanos(keepAliveTime);
         this.threadFactory = threadFactory;
         this.handler = handler;
     }
     ```

   - execute(...)

     ```java
     public void execute(Runnable command) {
         // 基本判空
         if (command == null)
             throw new NullPointerException();
         // 线程池状态
         int c = ctl.get();
         // ① 线程池线程数量小于核心线程数，添加核心线程
         if (workerCountOf(c) < corePoolSize) {
             // 提交任务，true表示核心线程，添加成功直接返回
             if (addWorker(command, true))
                 return;
             // 再次获取ctl，以防另外的线程对线程池进行状态修改
             c = ctl.get();
         }
         // ② 添加任务失败了,核心线程数满了,检查线程池处于运行状态，往队列添加任务
         if (isRunning(c) && workQueue.offer(command)) {
             // 再次获取 ctl 
             int recheck = ctl.get();
             // 检查是否还在运行状态，如果没有在运行了，remove掉当前任务
             if (! isRunning(recheck) && remove(command))
                 // 拒绝接收任务
                 reject(command);
             // 线程池线程数量为 0 了，没有核心线程数了
             else if (workerCountOf(recheck) == 0)
                 // 添加非核心线程来执行任务 false表示非核心线程,null表示补充线程
                 addWorker(null, false);
         }
         // ③ 队列满了，添加非核心线程执行任务
         else if (!addWorker(command, false))
             // ④ 如果失败，拒绝任务
             reject(command);
     }
     ```

   - addWorker(...)

     ```java
     // 创建线程并执行任务，core为 true表示创建核心线程，false表示创建临时线程
     private boolean addWorker(Runnable firstTask, boolean core) {
         retry:
         for (;;) {
             // 获取 ctl 值
             int c = ctl.get();
             // 线程池运行状态
             int rs = runStateOf(c);
     		// rs >= SHUTDOWN = SHUTDOWN/STOP/TIDING/TERMINATED 
             // rs == SHUTDOWN 不可以接收外部任务
             // first == null  外部提交任务为空
             // ! workQueue.isEmpty() 任务队列不为空
             // ① 一言以蔽之：线程池处于关闭状态，还有任务提交进来，直接返回 false 表示添加失败
             // firstTask == null 表示是添加补充线程来执行队列中的任务addWorker(null, false);
             if (rs >= SHUTDOWN &&
                 ! (rs == SHUTDOWN &&
                    firstTask == null &&
                    ! workQueue.isEmpty()))
                 return false;
     		// 这个循环就是添加线程数+1
             for (;;) {
                 // 获取线程池线程数量
                 int wc = workerCountOf(c);
                 // ② 线程数大于最大容量 
                 if (wc >= CAPACITY ||
                     // ③ 创建核心线程时线程数超过了corepoolSize，创建非核心线程数超过了最大线程数时
                     wc >= (core ? corePoolSize : maximumPoolSize))
                     // 直接返回 false 表示失败
                     return false;
                 // ④ CAS线程数+1，成功直接跳过大循环往后执行
                 if (compareAndIncrementWorkerCount(c))
                     // 跳过大循环，跳到下面的代码执行
                     break retry; 
                 // ⑤ CAS添加线程数失败了
                 // 再次获取 ctl 值
                 c = ctl.get();
                 // 查看当前线程池运行状态和前面的状态是否一致，不一致则重新从外层循环开始
                 if (runStateOf(c) != rs)
                     // 退出当前循环，跳到外层循环继续下一次循环
                     continue retry; 
                 // 一致则继续在内部循环运行
             }
         }
     	// 上面已经添加线程数成功了
         boolean workerStarted = false; // 任务已启动标志位
         boolean workerAdded = false; // 任务已添加标志位
         Worker w = null;
         try {
             // ① 包装任务，并创建线程  - 设置 AQS state = -1，禁止中断，直到 runWorker
             w = new Worker(firstTask);
             // 拿到线程
             final Thread t = w.thread;
             if (t != null) {
                 // 可重入锁，加锁
                 final ReentrantLock mainLock = this.mainLock;
                 mainLock.lock();
                 try {
                     // 拿到线程池运行状态
                     int rs = runStateOf(ctl.get());
     				// rs < SHUTDOWN = RUNNING 线程池运行中
                     if (rs < SHUTDOWN ||
                         // 补充线程
                         (rs == SHUTDOWN && firstTask == null)) {
                         // 线程已启动了，抛异常
                         if (t.isAlive()) 
                             throw new IllegalThreadStateException();
                         // ② 添加包装后的任务到 HashSet集合中
                         workers.add(w);
                         // 获取集合大小，并将此时的值赋值给 largestPoolSize
                         int s = workers.size();
                         if (s > largestPoolSize)
                             largestPoolSize = s;
                         // 修改已添加标志位
                         workerAdded = true;
                     }
                 } finally {
                     // 解锁
                     mainLock.unlock();
                 }
               // ③ 任务添加成功，启动线程
                 if (workerAdded) {
                     t.start();
                     // 修改已启动标志位
                     workerStarted = true;
                 }
             }
         } finally {
             // ④ 没有启动成功
             if (! workerStarted)
                 // 移除任务，并将线程数-1
                 addWorkerFailed(w);
         }
         // 返回是否启动成功结果
         return workerStarted;
     }
     ```
     
   - runWorker(...)

     ```java
     final void runWorker(Worker w) {
         // 当前worker线程
         Thread wt = Thread.currentThread();
         // 当前worker线程的任务
         Runnable task = w.firstTask;
         w.firstTask = null;
         w.unlock(); // addWorker时设置 -1禁止中断，到这里允许中断 - 设置 AQS state = 0
         boolean completedAbruptly = true;
         try {
             // task != null 表示是第一个任务
             // task == null 执行task = getTask() 从任务队列拿任务
             // ① 一言以蔽之：任务不为 null 的情况
             while (task != null || (task = getTask()) != null) {
                 // 加锁
                 w.lock();
                 // ② runStateAtLeast(ctl.get(), STOP) => rs >= STOP 线程池处于关闭状态
                 if ((runStateAtLeast(ctl.get(), STOP) ||
                      // 清空线程中断标志位 
                      (Thread.interrupted() &&
                       // 再次判断 rs >= STOP 线程池处于关闭状态
                       runStateAtLeast(ctl.get(), STOP))) &&
                     // ④ 消耗一个中断标志位（可能线程已被中断过的情况）
                     !wt.isInterrupted())
                     // 设置中断标志位
                     wt.interrupt();
                 
                 try {
                     // ⑤ 任务执行前钩子方法
                     beforeExecute(wt, task);
                     Throwable thrown = null;
                     try {
                         // ⑥ 执行任务
                         task.run();
                     } catch (RuntimeException x) {
                         thrown = x; throw x;
                     } catch (Error x) {
                         thrown = x; throw x;
                     } catch (Throwable x) {
                         thrown = x; throw new Error(x);
                     } finally {
                         // ⑦ 任务执行后钩子方法
                         //如果这个钩子抛异常将被吞掉,线程将会继续执行,需要自己注意处理代码捕获异常
                         afterExecute(task, thrown);
                     }
                 } finally {
                     // FOR GC
                     task = null;
                     // 完成任务数+1
                     w.completedTasks++;
                     // 任务执行完成 释放锁
                     w.unlock();
                 }
             }
             // getTask() 内部catch了异常，因此只有2个钩子的方法会导致 completedAbruptly=true
             // beforeExecute/afterExecute 会导致 completedAbruptly = true,用户导致的异常
             completedAbruptly = false;
         } finally {
             // 根据 completedAbruptly 处理情况不同的逻辑
             processWorkerExit(w, completedAbruptly);
         }
     }
     ```

   - getTask()

     ```java
     private Runnable getTask() {
         // 超时标志位
         boolean timedOut = false;
     	// 死循环
         for (;;) {
             // 拿到当前 ctl
             int c = ctl.get();
             // 线程池运行状态
             int rs = runStateOf(c);
             // 判断任务队列是否为空
             if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                 // 线程数-1
                 decrementWorkerCount();
                 return null;
             }
     		// 拿到线程数
             int wc = workerCountOf(c);
     		// 允许核心线程超时销毁或者线程数大于核心线程数
             boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
     		// 线程数大于最大线程数且超时了
             if ((wc > maximumPoolSize || (timed && timedOut))
                 // 线程数大于1，或任务队列空了
                 && (wc > 1 || workQueue.isEmpty())) {
                 // CAS线程数-1
                 if (compareAndDecrementWorkerCount(c))
                     return null;
                 continue;
             }
     
             try {
                 // timed 一般是 false 线程数大于了核心线程，带时间poll等待任务
                 // 否则阻塞等待任务 take()
                 Runnable r = timed ?
                     workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                 workQueue.take();
                 // 拿到任务就返回
                 if (r != null)
                     return r;
                 timedOut = true;
             } catch (InterruptedException retry) {
                 timedOut = false;
             }
         }
     }
     ```

   - processWorkerExit(...)

     ```java
     // 线程池线程退出情况处理 - 用户导致异常导致线程池线程退出，需要给线程池补充线程
     private void processWorkerExit(Worker w, boolean completedAbruptly) {
         // ① 用户导致了异常，线程池线程数-1
         if (completedAbruptly)
             decrementWorkerCount();
     	// 加锁操作
         final ReentrantLock mainLock = this.mainLock;
         mainLock.lock();
         try {
             completedTaskCount += w.completedTasks;
             // ② 从 workers 集合中移除当前的 worker
             workers.remove(w);
         } finally {
             // 解锁操作
             mainLock.unlock();
         }
     	// 尝试销毁
         tryTerminate();
     	// 获取线程池状态
         int c = ctl.get();
         // runStateLessThan(c, STOP) = RUNNING/SHUTDOWN
         if (runStateLessThan(c, STOP)) {
             // ③ 是否用户导致了线程，如果是，跳到addWorker(null, false);补充一个worker线程
             // 也即：用户导致了异常，线程池会新创建一个线程
             if (!completedAbruptly) {
                 // 获取核心线程数大小
                 int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                 // 核心线程数如果设置为 0 且任务队列还有任务,还需要线程继续执行任务的哟
                 if (min == 0 && ! workQueue.isEmpty())
                     // 设置最少还需要一个线程
                     min = 1;
                 // ④ 获取线程池当前的线程数和至少的线程数比较，如果大于表示线程池还有线程，直接返回
                 // 否则跳到 addWorker(null, false); 给线程池补充一个worker线程
                 if (workerCountOf(c) >= min)
                     return; // replacement not needed
             }
             // 给线程池补充一个worker线程
             addWorker(null, false);
         }
     }
     ```

   - shutdown()

     ```java
     // SHUTDOWN : 不接收任务，但是执行 workerQueue 中的任务
     public void shutdown() {
         // 加锁操作
         final ReentrantLock mainLock = this.mainLock;
         mainLock.lock();
         try {
             // 检查权限
             checkShutdownAccess();
             // 修改运行状态改为 SHUTDOWN
             advanceRunState(SHUTDOWN);
             // 中断空闲的线程
             interruptIdleWorkers();
             // 钩子方法
             onShutdown(); 
         } finally {
             // 解锁操作
             mainLock.unlock();
         }
         // TODO - 看后面
         tryTerminate();
     }
     
     // CAS改变线程池状态
     private void advanceRunState(int targetState) {
         // 死循环
         for (;;) {
             // 拿到 ctl
             int c = ctl.get();
             // 判断线程池是否至少处在了要改变的状态 rs >= targetState，是直接break 
             // 否则 CAS 改变线程池运行状态，如果失败继续下一次循环
             if (runStateAtLeast(c, targetState) ||
                 ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                 break;
         }
     }
     // 中断空闲线程
     private void interruptIdleWorkers() {
         interruptIdleWorkers(false);
     }
     private void interruptIdleWorkers(boolean onlyOne) {
         // 全局加锁操作
         final ReentrantLock mainLock = this.mainLock;
         mainLock.lock();
         try {
             // 循环 workers 集合
             for (Worker w : workers) {
                 // 拿到线程对象
                 Thread t = w.thread;
                 // 如果线程未被中断，且 tryLock成功表示空闲线程
                 if (!t.isInterrupted() && w.tryLock()) {
                     try {
                         // 中断线程
                         t.interrupt();
                     } catch (SecurityException ignore) {
                     } finally {
                         // unlock
                         w.unlock();
                     }
                 }
                 // 如果只是一个，break
                 if (onlyOne)
                     break;
             }
         } finally {
             // 全局锁释放
             mainLock.unlock();
         }
     }
     ```

   - shutdownNow()

     ```java
     // STOP : 不接收任务，也不执行 workerQueue 中的任务，可以拿到队列中的任务
     public List<Runnable> shutdownNow() {
         List<Runnable> tasks;
         // 加锁操作
         final ReentrantLock mainLock = this.mainLock;
         mainLock.lock();
         try {
             // 检查权限
             checkShutdownAccess();
             // 修改运行状态为 STOP
             advanceRunState(STOP);
             // 中断所有的线程
             interruptWorkers();
             // 取出队列中的任务进行返回
             tasks = drainQueue();
         } finally {
             // 解锁操作
             mainLock.unlock();
         }
         // TODO - 看后面
         tryTerminate();
         return tasks;
     }
     // 中断所有的线程
     private void interruptWorkers() {
         final ReentrantLock mainLock = this.mainLock;
         mainLock.lock();
         try {
             // 循环全部中断
             for (Worker w : workers)
                 w.interruptIfStarted();
         } finally {
             mainLock.unlock();
         }
     }
     
     void interruptIfStarted() {
         Thread t;
         // AQS的 state 状态值大于大于0时成立(防止线程池还没有启动就关闭的操作)
         // 线程不为null，且线程没有被中断过
         if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
             try {
                 // 中断线程
                 t.interrupt();
             } catch (SecurityException ignore) {
             }
         }
     }
     // 获取 workQueue 队列中的任务
     private List<Runnable> drainQueue() {
         BlockingQueue<Runnable> q = workQueue;
         ArrayList<Runnable> taskList = new ArrayList<Runnable>();
         // 这里是实现了drainTo方法的情况，取出队列的任务添加到list并移除任务
         q.drainTo(taskList);
         // 子类可能没有实现drainTo方法，所以需要以下的操作
         if (!q.isEmpty()) {
             // 取出 workQueue 队列中的任务进行移除并添加到 list集合中进行返回
             for (Runnable r : q.toArray(new Runnable[0])) {
                 if (q.remove(r))
                     taskList.add(r);
             }
         }
         return taskList;
     }
     ```

   - awaitTermination(...)

     ```java
     public boolean awaitTermination(long timeout, TimeUnit unit)
         throws InterruptedException {
         long nanos = unit.toNanos(timeout);
         // 加锁操作
         final ReentrantLock mainLock = this.mainLock;
         mainLock.lock();
         try {
             for (;;) {
                 // 查看线程池运行状态是否 TERMINATED，是直接返回，表示线程池已经终结
                 if (runStateAtLeast(ctl.get(), TERMINATED))
                     return true;
                 // 看下等待时间，小于等于不等待，直接返回
                 if (nanos <= 0)
                     return false;
                 // 条件等待队列等待被唤醒 termination Condition 条件等待队列
                 nanos = termination.awaitNanos(nanos);
             }
         } finally {
             // 解锁操作
             mainLock.unlock();
         }
     }
     ```

   - tryTerminate()

     ```java
     // 销毁方法
     final void tryTerminate() {
         for (;;) {
             // 当前 ctl
             int c = ctl.get();
             // 线程池是运行状态- 直接退出
             if (isRunning(c) ||
                 // 线程池处在 TIDING/TERMINATED - 直接退出
                 runStateAtLeast(c, TIDYING) ||
                 // 线程池状态处于 SHUTDOWN，此时不接收任务，但执行队列中的任务，判断队列是否为空
                 // 队列不为空，还有任务要执行 - 直接退出
                 (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                 return;
             // 线程池线程数不为 0 表示还有其他线程
             // 每个线程都会走这里，这里只需要确保一个线程继续往下走即可
             if (workerCountOf(c) != 0) {
                 // 中断worker线程 - 只中断一个[因为需要执行后续的代码，线程不能全部退出]
                 interruptIdleWorkers(ONLY_ONE);
                 return;
             }
     		// 加锁
             final ReentrantLock mainLock = this.mainLock;
             mainLock.lock();
             try {
                 // CAS 将线程池运行状态改为 TIDYING
                 if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                     try {
                         // 执行 terminated() 方法
                         terminated();
                     } finally {
                         // CAS 将线程池运行状态改为 TERMINATED
                         ctl.set(ctlOf(TERMINATED, 0));
                         // 唤醒 TERMINATED 状态的线程 awaitTerminated
                         termination.signalAll();
                     }
                     return;
                 }
             } finally {
                 // 解锁
                 mainLock.unlock();
             }
         }
     }
     ```

   

### ScheduledThreadPoolExecutor

1. HOW TO USE

   ```java
   public static void main(String[] args) throws InterruptedException {
   
   
       ScheduledExecutorService scheduledExecutorService =
           new ScheduledThreadPoolExecutor(2);
       // 延迟执行1次
       scheduledExecutorService.schedule(() ->
                   System.out.println(new Date()),2,TimeUnit.SECONDS);
      	/**
        * Sat Jun 19 15:48:47 CST 2021
        * Sat Jun 19 15:48:49 CST 2021
        * Sat Jun 19 15:48:51 CST 2021
        */
       // 周期执行：任务开始时开始计时
       scheduledExecutorService.scheduleAtFixedRate(() -> {
           System.out.println(new Date());
           try {
               Thread.sleep(2000);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       },0,2,TimeUnit.SECONDS);
   
      /**
       * Sat Jun 19 15:48:03 CST 2021
       * Sat Jun 19 15:48:07 CST 2021
       * Sat Jun 19 15:48:11 CST 2021
       */
       // 周期执行：任务结束时开始计时
       scheduledExecutorService.scheduleWithFixedDelay(() -> {
           System.out.println(new Date());
           try {
               Thread.sleep(2000);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       },0,2,TimeUnit.SECONDS);
   }
   ```

2. ##### ScheduledThreadPoolExecutor

   ![1624074838652](G:\JDK源码\ThreadPoolExecutor.assets\1624074838652.png)

3. ##### ScheduleFutureTask 任务对象

   ![1624075344402](G:\JDK源码\ThreadPoolExecutor.assets\1624075344402.png)

4. ##### 任务提交

   ![1624075380327](G:\JDK源码\ThreadPoolExecutor.assets\1624075380327.png)

5. ##### 接口方法

   ```java
   public interface ScheduledExecutorService extends ExecutorService {
   
       public ScheduledFuture<?> schedule(Runnable command,
                                          long delay, TimeUnit unit);
   
   
       public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                              long delay, TimeUnit unit);
   	
   	// 任务成功执行完后计算时间 - 不包含执行时间
       public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                     long initialDelay,
                                                     long period,
                                                     TimeUnit unit);
   	// 包含执行时间
       public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                        long initialDelay,
                                                        long delay,
                                                        TimeUnit unit);
   
   }
   ```

6. ##### 源码分析 - 几个主要的方法

   - 构造器

     ```java
     public ScheduledThreadPoolExecutor(int corePoolSize) {
         super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
               // 无界延迟队列
               new DelayedWorkQueue());
     }
     ```

   - ScheduledFutureTask

     ```java
     // 比较器
     public interface Delayed extends Comparable<Delayed> {
         // 获取延迟时间
         long getDelay(TimeUnit unit);
     }
     
     public interface ScheduledFuture<V> extends Delayed, Future<V> {
     
     }
     
     public interface RunnableScheduledFuture<V> extends RunnableFuture<V>, ScheduledFuture<V> {
         // 是否周期性执行
         boolean isPeriodic();
     }
     
     private class ScheduledFutureTask<V>
                 extends FutureTask<V> implements RunnableScheduledFuture<V> {
         
     }
     ```

     

   - schedule(...)

     ```java
     public ScheduledFuture<?> schedule(Runnable command,
                                        long delay,
                                        TimeUnit unit) {
         if (command == null || unit == null)
             throw new NullPointerException();
         RunnableScheduledFuture<?> t = decorateTask(command,
                                                     new ScheduledFutureTask<Void>(command, null,
                                                                                   triggerTime(delay, unit)));
         delayedExecute(t);
         return t;
     }
     ```

   - take()

     ```java
     // 延迟任务获取
     public RunnableScheduledFuture<?> take() throws InterruptedException {
         final ReentrantLock lock = this.lock;
         lock.lockInterruptibly();
         try {
             for (;;) {
                 RunnableScheduledFuture<?> first = queue[0];
                 if (first == null)
                     available.await();
                 else {
                     long delay = first.getDelay(NANOSECONDS);
                     if (delay <= 0)
                         return finishPoll(first);
                     first = null; // don't retain ref while waiting
                     if (leader != null)
                         available.await();
                     else {
                         Thread thisThread = Thread.currentThread();
                         leader = thisThread;
                         try {
                             available.awaitNanos(delay);
                         } finally {
                             if (leader == thisThread)
                                 leader = null;
                         }
                     }
                 }
             }
         } finally {
             if (leader == null && queue[0] != null)
                 available.signal();
             lock.unlock();
         }
     }
     ```

   - scheduleAtFixedRate(...)

     ```java
     public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                   long initialDelay,
                                                   long period,
                                                   TimeUnit unit) {
         if (command == null || unit == null)
             throw new NullPointerException();
         if (period <= 0)
             throw new IllegalArgumentException();
         ScheduledFutureTask<Void> sft =
             new ScheduledFutureTask<Void>(command,
                                           null,
                                           triggerTime(initialDelay, unit),
                                           unit.toNanos(period));
         RunnableScheduledFuture<Void> t = decorateTask(command, sft);
         sft.outerTask = t;
         delayedExecute(t);
         return t;
     }
     ```

   - scheduleWithFixedDelay(...)

     ```java
     public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                      long initialDelay,
                                                      long delay,
                                                      TimeUnit unit) {
         if (command == null || unit == null)
             throw new NullPointerException();
         if (delay <= 0)
             throw new IllegalArgumentException();
         ScheduledFutureTask<Void> sft =
             new ScheduledFutureTask<Void>(command,
                                           null,
                                           triggerTime(initialDelay, unit),
                                           unit.toNanos(-delay));
         RunnableScheduledFuture<Void> t = decorateTask(command, sft);
         sft.outerTask = t;
         delayedExecute(t);
         return t;
     }
     ```

   - delayedExecute(...)

     ```java
     private void delayedExecute(RunnableScheduledFuture<?> task) {
         if (isShutdown())
             reject(task);
         else {
             super.getQueue().add(task);
             if (isShutdown() &&
                 !canRunInCurrentRunState(task.isPeriodic()) &&
                 remove(task))
                 task.cancel(false);
             else
                 ensurePrestart();
         }
     }
     ```

     































