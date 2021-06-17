## FJP的全源码逐行分析

希望读者：对每个方法了解掌握后，进行总结在方法的后面。以及将方法分离罗列成二级标题。

```java
public ForkJoinPool() {
    // MAX_CAP      = 0x7fff  32767; 所以最大线程数。默认就是CPU核心数
    this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()),
         defaultForkJoinWorkerThreadFactory, null, false);
}
public ForkJoinPool(int parallelism,
                    ForkJoinWorkerThreadFactory factory, // 线程工厂
                    UncaughtExceptionHandler handler, // 线程异常处理器
                    boolean asyncMode) { // 执行模式：异步或者同步。默认是false
    this(checkParallelism(parallelism), // 非法参数校验
         checkFactory(factory),
         handler,
         asyncMode ? FIFO_QUEUE : LIFO_QUEUE, // 取任务的方式：base和top
         "ForkJoinPool-" + nextPoolId() + "-worker-");
    checkPermission();
}
// 最终构造器
private ForkJoinPool(int parallelism,
                     ForkJoinWorkerThreadFactory factory,
                     UncaughtExceptionHandler handler,
                     int mode,
                     String workerNamePrefix) {
    this.workerNamePrefix = workerNamePrefix;
    this.factory = factory;
    this.ueh = handler;
    this.config = (parallelism & SMASK) | mode; // SMASK = 0x0000 ffff  将config的低16位用于存放并行度
    // static final int MODE_MASK    = 0xffff << 16;  // 高16位保存mode
    // static final int LIFO_QUEUE   = 0; // 栈结构不需要占用位
    // static final int FIFO_QUEUE   = 1 << 16;
    long np = (long)(-parallelism); //以并行度64为例 AC 1111111111000000   TC： 1111111111000000
    this.ctl = ((np << AC_SHIFT) & AC_MASK) | ((np << TC_SHIFT) & TC_MASK);
}

// 将任务封装成FJT，因为FJP只执行FJT的任务
public ForkJoinTask<?> submit(Runnable task) {
        ForkJoinTask<?> job;
        if (task instanceof ForkJoinTask<?>)
            job = (ForkJoinTask<?>) task;
        else
            job = new ForkJoinTask.AdaptedRunnableAction(task);
        externalPush(job); // 核心提交任务方法
        return job；
}
final void externalPush(ForkJoinTask<?> task) {
      externalSubmit(task); // 核心方法
}
// 真正执行提交任务的方法
private void externalSubmit(ForkJoinTask<?> task) {
    int r;                                    // 随机数
    // 取随机数
    for (;;) { // 如果出现这种死循环，必然会有多个判断条件，多个执行分支
        WorkQueue[] ws; WorkQueue q; int rs, m, k;
        boolean move = false;
        if ((rs = runState) < 0) { // 判断线程池是否已经关闭
            tryTerminate(false, false);  
            throw new RejectedExecutionException();
        }
        else if ((rs & STARTED) == 0 || // STARTED状态位没有置位，所以需要初始化
                 ((ws = workQueues) == null || (m = ws.length - 1) < 0)) { // workQueues需要初始化
            // 注意：如果该分支不执行，但是由于||运算符的存在，这里ws和m变量已经初始化
            int ns = 0;
            rs = lockRunState();
            try {
                if ((rs & STARTED) == 0) { // 再次判断了一下STARTED状态位，为何？因为多线程操作，可能上面判断过后，已经被别的线程初始化了，所以没必要再进行CAS
                    U.compareAndSwapObject(this, STEALCOUNTER, null,
                                           new AtomicLong());
                    // p就是并行度：工作线程的数量
                    int p = config & SMASK; // ensure at least 4 slots
                    int n = (p > 1) ? p - 1 : 1; // n最小为1
                    // 就是取传入的数，最近的2的倍数
                    1 |= n >>> 1; n |= n >>> 2;  n |= n >>> 4;
                    n |= n >>> 8; n |= n >>> 16; n = (n + 1) << 1;
                    workQueues = new WorkQueue[n]; // 初始化workQueue队列（外部提交队列+工作窃取队列）
                    ns = STARTED; // 初始化成功，那么此时状态修改为STARTED状态
                }
            } finally {
                unlockRunState(rs, (rs & ~RSLOCK) | ns); // 最后释放锁
            }
        }
        // 在上一个分支执行完毕后，第二次循环将会到达这里。由于咱们的算法是用了一个全局队列:workQueues来存储两个队列：外部提交队列、内部工作队列（任务窃取队列），那么这时，是不是应该去找到我这个任务放在哪个外部提交队列里面。就是通过上面获取的随机种子r，来找到应该放在哪里？SQMASK = 1111110，所以由SQMASK的前面的1来限定长度，末尾的0来表明，外部提交队列一定在偶数位
        else if ((q = ws[k = r & m & SQMASK]) != null) {
            // 由于当前提交队列是外部提交队列，那么一定会有多线程共同操作，那么为了保证并发安全，那么这里需要上锁，也即对当前提交队列进行锁定
            if (q.qlock == 0 && U.compareAndSwapInt(q, QLOCK, 0, 1)) {
                ForkJoinTask<?>[] a = q.array; // 取提交队列的保存任务的数组array
                int s = q.top; // doug lea 大爷发现，从top往下放，类似于压栈过程，而栈顶指针就叫sp（stack pointer），所以简写为s
                boolean submitted = false; // initial submission or resizing
                try {                      // locked version of push
                    if (
                        (
                        a != null && // 当前任务数组已经初始化
                            a.length > s + 1 - q.base // 判断数组是否已经满了
                        ) 
                        ||
                        (a = q.growArray()) != null) { // 那么就初始化数组或者扩容
                        // 扩容之后，开始存放数据
                        int j = (((a.length - 1) & s) << ASHIFT) + ABASE; // j就是s也就是栈顶的绝对地址
                        U.putOrderedObject(a, j, task); // 放数据
                        U.putOrderedInt(q, QTOP, s + 1); // 对栈顶指针+1
                        // 为什么这里需要怎么写？注意：qtop和q.array没有volatile修饰
                        submitted = true;
                    }
                } finally {
                    U.compareAndSwapInt(q, QLOCK, 1, 0); // qlock是volatile的，由于volatile的特性，这个操作CAS出去，那么qlock线程可见，必然上面的task和qtop可见，且有序。
                }
                // 由于添加成功了，但是，没有工作线程，那么这时通过signalWork，创建工作线程并执行
                if (submitted) {
                    signalWork(ws, q);
                    return;
                }
            }
            move = true; // 由于当前线程无法获取初始计算的提交队列的锁，那么这时发生了线程竞争，那么设置move标志位，让线程在下一次循环的时候，重新计算随机数，让它寻找另外的队列。
        }
        else if (((rs = runState) & RSLOCK) == 0) { // 如果找到的这个wq没有被创建，那么创建他，但是，这里的RSLOCK的判断，在于，当没有别的线程持有RSLOCK的时候，才会进入。这是由于RSLOCK主管，runstate，可能有别的线程把状态改了，根本不需要再继续work了
            q = new WorkQueue(this, null); // 创建外部提交队列，由于ForkJoinWorkerThread FJWT为null，所以为外部提交队列
            q.hint = r; // r为什么保存为hint，r是随机数，通过r找到当前外部提交队列，处于WQS的索引下标
            q.config = k | SHARED_QUEUE; // SHARED_QUEUE = 1 << 31；这里就是将整形int的符号位置1，所以为负数，SHARED_QUEUE表明当前队列是共享队列（外部提交队列）。而k为当前wq处于wqs中的索引下标
            q.scanState = INACTIVE; // 由于当前wq并没有进行扫描任务，所以扫描状态位无效状态INACTIVE
            rs = lockRunState();           // 对wqs上锁操作：就是讲上面的队列放入到wqs的偶数位中
            if (rs > 0 &&  // 确保线程池处于运行状态
                (ws = workQueues) != null &&
                k < ws.length && ws[k] == null) // 由于可能两个线程同时进来操作，只有一个线程持有锁，那么只允许一个线程放创建的队列，但是这里需要注意的是：可能会有多个线程创建了WorkQueue，但是只有一个能成功
                ws[k] = q;     // 将wq放入全局队列wqs中
            unlockRunState(rs, rs & ~RSLOCK); // 解锁
        }
        else // 发生竞争时，让当前线程选取其他的wq来重试
            move = true;                 
        if (move)
            r = ThreadLocalRandom.advanceProbe(r); // 获取下一个不同的随机数
    }
}
// 上面的内容，都是初始化，放，but，执行线程没有啊，谁来执行这个放入到了外部提交队列中的任务。
final void signalWork(WorkQueue[] ws, WorkQueue q) {
    long c; int sp, i; WorkQueue v; Thread p;
    while ((c = ctl) < 0L) {                       // 符号位没有溢出：最高16位为AC，代表了工作活跃的线程数没有达到最大值
        if ((sp = (int)c) == 0) {                  // ctl的低32位代表了INACTIVE数。若此时sp=0，代表了没有空闲线程
            if ((c & ADD_WORKER) != 0L)            // ADD_WORKER = 0x0001L << (TC_SHIFT + 15) TC的最高位是不是0，若不是0，那么FJP中的工作线程代表了没有达到最大线程数
                tryAddWorker(c); // 尝试添加工作线程
            break;
        }
        // 此时，代表了空闲线程不为null
        if (ws == null)                            // 此时FJP的状态为NOT STARTED，TERMINATED
            break;
        if (ws.length <= (i = sp & SMASK))         // SMASK = 0xffff 取sp的低16位 TERMINATED
            break;
        if ((v = ws[i]) == null)                   // ctl低32位的低16位是不是存放了INACTIVE线程在wqs的下标i  TERMINATING
            break;
        int vs = (sp + SS_SEQ) & ~INACTIVE;        // SS_SEQ = 1 << 16; ctl低32位的高16位是不是存放了版本计数 version count   INACTIVE= 1 << 31;
        int d = sp - v.scanState;                  // screen CAS
        long nc = (UC_MASK & (c + AC_UNIT)) | // 把获取到的INACTIVE的线程，也即空闲线程唤醒，那么唤醒后，是不是应该对AC + 1（加1操作(c + AC_UNIT)），UC_MASK为高32位1，低32位0，所以(UC_MASK & (c + AC_UNIT))代表了，保留ctl的高32位值，也即AC+1和TC值
            (SP_MASK & v.stackPred); // SP_MASK为高32位0，低32位1，所以保留了低32位的值，v.stackPred 代表了一个出栈操作，让低32位的低16位更新为唤醒线程的下一个线程
        // 此时的nc就是计算好的下一个ctl，next ctl -> nc
        if (d == 0 && U.compareAndSwapLong(this, CTL, c, nc)) { // CAS 替换CTL
            v.scanState = vs;           // 记录版本信息放入scanState，此时为正数
            if ((p = v.parker) != null) // 唤醒工作线程
                U.unpark(p);
            break;
        }
        if (q != null && q.base == q.top)    // 队列为空，直接退出。由于队列是多线程并发的，所以有可能放入其中的任务已经被其他线程获取，所以此时队列为空
            break;
    }
}
// 尝试添加工作线程
private void tryAddWorker(long c) {
    boolean add = false;
    do {
        long nc = ((AC_MASK & (c + AC_UNIT)) | // active count +1 活跃线程数加1，此时只保留了高32位的高16位信息
                   (TC_MASK & (c + TC_UNIT))); // total count +1 总线程数加1，此时只保留了高32位的低16位信息
        // 此时nc为next ctl，也即活跃线程数+1，总线程数+1
        if (ctl == c) { // ctl没有被其他线程改变
            int rs, stop;           
            // 上锁并检查FJP的状态是否为STOP
            if ((stop = (rs = lockRunState()) & STOP) == 0) 
                add = U.compareAndSwapLong(this, CTL, c, nc); // 更新ctl的值。add表明是否添加成功
            unlockRunState(rs, rs & ~RSLOCK); // 解锁
            if (stop != 0) // 线程池已经停止
                break;
            if (add) { // 如果ac和tc添加1成功，也即nc替换成功，那么创建工作线程
                createWorker();
                break;
            }
        }
    } while (((c = ctl) & ADD_WORKER) != 0L  // 没有达到最大线程数
             && (int)c == 0); // 低32位0，如果有空闲线程，你添加他干甚？
}
// 创建工作线程
private boolean createWorker() {
    ForkJoinWorkerThreadFactory fac = factory;
    Throwable ex = null;
    ForkJoinWorkerThread wt = null;
    try {
        if (fac != null && (wt = fac.newThread(this)) != null) { // 从线程工厂中，创建线程
            wt.start(); // 并启动线程
            return true;
        }
    } catch (Throwable rex) {
        ex = rex;
    }
    deregisterWorker(wt, ex); // 如果创建出现异常，将ctl前面加的1回滚
    return false;
}
// 默认线程工厂
static final class DefaultForkJoinWorkerThreadFactory
    implements ForkJoinWorkerThreadFactory {
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new ForkJoinWorkerThread(pool); // 直接new ForkJoinWorkerThread
    }
}
// FJWT的构造器
protected ForkJoinWorkerThread(ForkJoinPool pool) {
    super("aForkJoinWorkerThread");
    this.pool = pool; // 保存对外的FJP的引用 
    this.workQueue = pool.registerWorker(this); // 将自己注册到FJP中，其实就是保存到FJP的奇数位中
}
// 将自己注册到FJP中
final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
    UncaughtExceptionHandler handler;
    wt.setDaemon(true);                           //  FJWT的守护状态为守护线程
    if ((handler = ueh) != null) // 如果设置了线程的异常处理器，那么设置
        wt.setUncaughtExceptionHandler(handler);
    WorkQueue w = new WorkQueue(this, wt); // 创建工作线程，注意：创建外部提交队列时： WorkQueue w = new WorkQueue(this, null)
    int i = 0;                                    // 创建的工作队列所保存在wqs的索引下标
    int mode = config & MODE_MASK;                // 取设置的工作模式：FIFO、LIFO
    int rs = lockRunState(); // 上锁
    try {
        WorkQueue[] ws; int n;                    
        if ((ws = workQueues) != null && (n = ws.length) > 0) { // 日常判空操作
            int s = indexSeed += SEED_INCREMENT;  // indexSeed = 0 SEED_INCREMENT = 0x9e3779b9 ，减少hash碰撞
            int m = n - 1; // wqs长度-1，用于取模运算
            i = ((s << 1) | 1) & m;               // 找存放w的下标
            if (ws[i] != null) {                  // 发生了碰撞
                int probes = 0;                   // step by approx half n
                int step = (n <= 4) ? 2 : ((n >>> 1) & EVENMASK) + 2; // 发生碰撞二次寻址 EVENMASK     = 0xfffe;
                while (ws[i = (i + step) & m] != null) { // step保证为偶数，一个奇数+偶数一定等于奇数
                    if (++probes >= n) { // 寻址达到了极限，那么扩容
                        workQueues = ws = Arrays.copyOf(ws, n <<= 1); // 扩容容量为2倍
                        m = n - 1;
                        probes = 0;
                    }
                }
            }
            w.hint = s;                           // s作为随机数保存在wq的hint中
            w.config = i | mode;                  // 保存索引下标 + 模式
            w.scanState = i;                      // scanState为volatile，此时对它进行写操作，ss写成功，上面的变量一定可见，且不会和下面的ws[i]赋值发生重排序。注意这里的scanState就变成了odd，也即奇数，所以要开始扫描获取任务并执行啦
            ws[i] = w; // 放入全局队列中
        }
    } finally {
        unlockRunState(rs, rs & ~RSLOCK); // 解锁
    }
    wt.setName(workerNamePrefix.concat(Integer.toString(i >>> 1)));
    return w;
}
// FJWT的run方法，执行体
public void run() {
    if (workQueue.array == null) { // 只run一次。array用于存放FJT，那么这里用它作为标识，来确保FJWT只run一次
        Throwable exception = null;
        try {
            onStart(); // run之前执行钩子函数
            pool.runWorker(workQueue); // 核心方法
        } catch (Throwable ex) {
            exception = ex;
        } finally {
            try {
                onTermination(exception);  // run之后执行钩子函数
            } catch (Throwable ex) {
                if (exception == null) // 如果异常为空，则保存这里的异常，否则异常为上面的catch块异常。其实呢就是一句话：保存最先发生的异常
                    exception = ex;
            } finally {
                pool.deregisterWorker(this, exception);  // 线程退出后，进行状态还原
            }
        }
    }
}
// FJP中的真正处理FJWT工作的函数
final void runWorker(WorkQueue w) {
    w.growArray();                   // 分配array
    int seed = w.hint;               // 取随机数
    int r = (seed == 0) ? 1 : seed;  // 避免出现0
    for (ForkJoinTask<?> t;;) { // 循环获取任务并执行，直到显示退出
        if ((t = scan(w, r)) != null) // 扫描可执行任务
            w.runTask(t); // 拿到任务之后开始执行
        else if (!awaitWork(w, r)) // 如果没有任务可执行，那么awaitWork等待任务执行
            break; // 显式退出
        r ^= r << 13; r ^= r >>> 17; r ^= r << 5; // 异或算法，基于上一个随机数r，计算下一个伪随机数
    }
}
// WorkQueue类中用于初始化或者增长array函数
final ForkJoinTask<?>[] growArray() {
    ForkJoinTask<?>[] oldA = array;
    // oldA存在，那么进行二倍长度扩容，否则size为初始化大小INITIAL_QUEUE_CAPACITY = 1 << 13
    int size = oldA != null ? oldA.length << 1 : INITIAL_QUEUE_CAPACITY;
    // 如果扩容之后，超过最大容量MAXIMUM_QUEUE_CAPACITY = 1 << 26;也即64M，抛出异常
    if (size > MAXIMUM_QUEUE_CAPACITY)
        throw new RejectedExecutionException("Queue capacity exceeded");
    int oldMask, t, b;
    // 创建新的array数组
    ForkJoinTask<?>[] a = array = new ForkJoinTask<?>[size];
    // 扩容后，需要干嘛？需要将旧的array中的任务放入到新的数组中
    // 面试题：为什么这里手动复制，而不用更快速的复制？System.arraycopy？面的是什么？？如何保证数组中的元素的可见性？getObjectVolatile（数组的首地址，偏移量）
    if (oldA != null && (oldMask = oldA.length - 1) >= 0 &&
        (t = top) - (b = base) > 0) {
        int mask = size - 1;
        do { 
            ForkJoinTask<?> x;
            // 注意：这里从base引用开始取任务
            int oldj = ((b & oldMask) << ASHIFT) + ABASE;
            int j    = ((b &    mask) << ASHIFT) + ABASE;
            x = (ForkJoinTask<?>)U.getObjectVolatile(oldA, oldj); // volatile 语义 1
            if (x != null &&
                U.compareAndSwapObject(oldA, oldj, x, null)) // 这里为何使用CAS？因此由于队列是工作窃取队列，可能有别的线程持有old数组引用，正在通过base引用窃取尾部任务
                U.putObjectVolatile(a, j, x);  // volatile 语义 2
        } while (++b != t); // 循环，直到转移成功
    }
    return a;
}
// FJP中，在FJWT进行扫描获取任务执行。w为当前FJWT所处队列，r为随机数
private ForkJoinTask<?> scan(WorkQueue w, int r) {
    WorkQueue[] ws; int m;
    if ((ws = workQueues) != null && (m = ws.length - 1) > 0 && w != null) { // 日常判空
        int ss = w.scanState;                     // 保存初始时的扫描状态
        // 扫描获取任务。origin初始为随机数取模的下标，k初始为origin。
        // 由于在扫描过程中，可能有别的线程添加获取等等操作，那么我怎么样让当前FJWT返回呢？不可能一直在这扫描吧，实在是没有任务了，那么必须退出，避免造成性能损耗，那么问题就变为：如何发现当前没有可执行的任务呢？（这叫推理学习，记忆方式），所以采用oldSum和checkSum来判断整个wqs是否处于稳定状态，也即没有别的线程再往里添加任务了，而且经历过一个周期，没有扫描到可执行任务，即可退出。
        for (int origin = r & m, k = origin, oldSum = 0, checkSum = 0;;) {
            WorkQueue q; ForkJoinTask<?>[] a; ForkJoinTask<?> t;
            int b, n; long c;
            // 哈哈，在wqs中找到了一个不为空的队列。那么看看有没有可以获取任务
            if ((q = ws[k]) != null) { 
                if ((n = (b = q.base) - q.top) < 0 && // 由于放任务将会操作top指针，初始时，base等于top，每添加一个任务，top加1，所以随着任务的添加，那么此时base落后于top，所以base - top < 0 表明队列有任务
                    (a = q.array) != null) {      // array队列不为空，表明有任务可以获取
                    long i = (((a.length - 1) & b) << ASHIFT) + ABASE; // 取array索引下标base的偏移地址
                    if ((t = ((ForkJoinTask<?>)
                              U.getObjectVolatile(a, i))) != null && // 任务存在
                        q.base == b) { // base引用没有被改变，也即任务没有被取走
                        if (ss >= 0) { // 如果扫描状态正常
                            // CAS取任务
                            if (U.compareAndSwapObject(a, i, t, null)) {
                                q.base = b + 1; // 增加base值
                                if (n < -1)       // 队列大于一个任务，那么其他线程赶紧起来干活
                                    signalWork(ws, q);
                                return t; // 返回获取的任务
                            }
                        }
                        else if (oldSum == 0 &&   
                                 w.scanState < 0) // oldSum未改变之前，才能判断w的扫描状态，如果扫描状态小于0，代表INACITVE，此时需要尝试唤醒空闲线程进行扫描工作
                            // c最新的ctl值，ws[m & (int)c]栈顶的索引下标，AC_UNIT 用于计算活跃线程数
                            tryRelease(c = ctl, ws[m & (int)c], AC_UNIT);
                    }
                    // 任务不存在，那么判断，如果此时扫描状态处于INACTIVE的话，那么需要重新获取扫描状态，可能别的线程已经将其置为扫描状态
                    if (ss < 0)      
                        ss = w.scanState;
                    // 执行到这里，那么只可能是因为线程竞争导致的，所以为了减少竞争，那么重新计算随机数，转移获取任务的wq，复位origin，oldSum、checkSum 方便计算轮回
                    r ^= r << 1; r ^= r >>> 3; r ^= r << 10;
                    origin = k = r & m;        
                    oldSum = checkSum = 0;
                    continue;
                }
                checkSum += b; // 通过base数值来进行校验和计算
            }
            // 扫描wqs正好经历过一个周期
            if ((k = (k + 1) & m) == origin) {    // continue until stable
                if ((ss >= 0 || (ss（这时老的） == (ss（这时新的） = w.scanState))) && // 工作线程处于活跃状态，或者状态没有改变，注意：这里进行了ss的状态更新 ss = w.scanState
                    oldSum（这时老的） == (oldSum（这时新的） = checkSum)) { // 旧的oldSum 和 新的 checkSum 比较，同时更新oldSum
                    // 总结: stable 稳定态： 扫描状态不变 且 没有线程操作队列
                    if (ss < 0 || w.qlock < 0)    // 工作线程切换为 INACTIVE且队列稳定，所以退出即可
                        break;
                    int ns = ss | INACTIVE;       // 将扫描状态设置为INACTIVE
                    long nc = ((SP_MASK & ns) |
                               (UC_MASK & ((c = ctl) - AC_UNIT))); // 活跃线程数减1
                    // CTL低32位就是空闲线程栈的栈顶。workqueue的stackPred就是栈中的空闲线程
                    w.stackPred = (int)c;         // 之前栈顶空闲线程的索引下标+版本号
                    // 优化到了极致。由于这里是volatile，进行直接赋值将会导致StoreStore和StoreLoad屏障，所以用unsafe类来普通变量赋值，减少性能损耗，而后面的CAS操作后，自然能够保证这里的scanState语义。
                    U.putInt(w, QSCANSTATE, ns);
                    // CAS 替换ctl值
                    if (U.compareAndSwapLong(this, CTL, c, nc))
                        ss = ns; // 这里又是一个优化点，因为CTL替换成功，必然scanState写成功，那么局部变量直接更新为最新值，而不用再去读scanState变量
                    else
                        w.scanState = ss;         // 如果失败了，那么回退到原来的状态，因为CTL没有改变，也即active count没有减1成功，自然scanState需要回退。问：这里为什么直接写volatile了？
                }
                checkSum = 0;
            }
        }
    }
    return null;
}
// WorkQueue中执行scan获取到的FJT
final void runTask(ForkJoinTask<?> task) {
    if (task != null) { // 日常判空
        scanState &= ~SCANNING; // mark as busy 标记当前wq的工作线程处于执行获取到的任务状态。即标记为偶数。取scanState的高31位。int SCANNING  = 1
        (currentSteal = task).doExec(); // task是不是当前线程从队列里面获取的（scan），也即将task设置为currentSteal。这里先不要了解FJT的内容，只需要知道运行了，怎么运行的。不需要了解怎么去实现FJT等等，后面再说。
        U.putOrderedObject(this, QCURRENTSTEAL, null); // 执行完毕之后释放currentSteal引用。为什么这里这么写？store buffer -> StoreLoad -> volatile语义？写变量时，StoreStore,StoreLoad。那么这里为了保证写入顺序-> putOrderedObject 避免了StoreLoad屏障对性能的损耗。
        // todo
        execLocalTasks(); // 直接翻译：执行本地任务。为何执行本地任务？考虑一个问题：谁能往当前线程的工作队列里放任务？当前线程在执行FJT时往自己队列里放了任务，也只有当前线程才能往array任务数组里放任务。
        ForkJoinWorkerThread thread = owner;
        if (++nsteals < 0)      // nsteals代表了当前线程总的偷取的任务数量。由于符号限制，所以检查是否发生符号溢出
            transferStealCount(pool); // 当前线程32位计数值达到饱和，那么将其加到FJP的全局变量的64位计数器中，并且清零计数值 nsteals
        scanState |= SCANNING; // 任务执行完成，恢复扫描状态
        if (thread != null)
            thread.afterTopLevelExec(); // 任务执行完的钩子函数
    }
}
// FJP用于FJWT工作线程等待唤醒的方法
private boolean awaitWork(WorkQueue w, int r) {
    if (w == null || w.qlock < 0)                 // 线程池正在Terminate
        return false; // 返回false 直接退出runWorker内部循环，也即退出FJWT
    // 取当前工作线程压入空闲栈中的前一个工作线程版本号+下标（32位切割为：高16 + 低16）赋值于pred，SPINS代表自旋次数为0
    for (int pred = w.stackPred, spins = SPINS, ss;;) {
        if ((ss = w.scanState) >= 0) // 当前工作线程状态已经被修改为ACTIVE状态，那么赶紧干活去
            break;
        else if (spins > 0) { // 没有达到自旋次数阈值
            r ^= r << 6; r ^= r >>> 21; r ^= r << 7;
            if (r >= 0 && --spins == 0) {         // randomize spins
                WorkQueue v; WorkQueue[] ws; int s, j; AtomicLong sc;
                if (pred != 0 && (ws = workQueues) != null &&
                    (j = pred & SMASK) < ws.length &&
                    (v = ws[j]) != null &&        // see if pred parking
                    (v.parker == null || v.scanState >= 0))
                    spins = SPINS;                // continue spinning
            }
        }
        else if (w.qlock < 0)                     // 自旋之后再次检测下线程池状态
            return false;
        else if (!Thread.interrupted()) { //  如果当前FJWT工作线程没有发生中断，那么尝试睡眠，否则清除中断标志位后继续scan扫描任务，干活去
            long c, prevctl, parkTime, deadline;
            // 之前我们以并行度64为例 AC（高32位的高16位） 1111 1111 1100 0000  64并行度：0000 0000 0100 0000        
            int ac = (int)((c = ctl) >> AC_SHIFT) +  // AC_SHIFT = 48 符号右移。获取高16位的值：活跃线程数
                (config & SMASK); // 取低16位config的值：在构造函数中设置的并行度（这里是64）。
            //   SMASK = 0xffff
            // 总结：用64位的ctl的高32位的高16位与设置的并行度相加可以得到ac，也即活跃线程数
            // 此时 ac = 0 。那么代表了活跃线程数为0。活跃线程数为0之后，我需要看看FJP有没有设置SHUTDOWN或者STOP。此时需要调用tryTerminate看看自己是不是最后一个线程，然后做清理和进一步的操作。
            if ((ac <= 0 && tryTerminate(false, false)) ||
                (runState & STOP) != 0)           // 线程池状态处于STOP停止态，所以停止执行
                return false;
            // 看看当前线程是不是最后一个空闲线程
            if (ac <= 0 && ss == (int)c) {       
                prevctl = (UC_MASK & (c + AC_UNIT)) | // 为什么这里要加1？我当前线程结束了吗？在我FJP线程代码没有执行完毕之前我还是属于FJP的，所以还是属于活跃线程。这里TC都还没改呢？
                    (SP_MASK & pred); // 因为当前线程要释放资源，不属于FJP了，所以栈顶还原到之前的索引下标
                // 取CTL中的总线程数：活跃线程数（AC）+非活跃线程数（空闲线程数）
                int t = (short)(c >>> TC_SHIFT);  
                // 保证至少有三个空闲线程即可。由于当前线程池处于静默状态，如果持有太多空闲线程将会浪费系统资源，那么何不如把线程释放掉，而又需要保持线程池的活性，即有任务也能不创建线程执行，那么这里设置阈值为3个空闲线程数。这里写法不规范，理论上来说这是可以调节的，设置为全局变量比较合适。
                if (t > 2 &&  // 为什么t>2见上解释
                    U.compareAndSwapLong(this, CTL, c, prevctl)) // 如果这里t>2，那么表明有足够多的空闲线程，那么我需要将CTL还原到 + 1 ac 和 之前栈顶的线程索引。
                    return false;                 
                parkTime = IDLE_TIMEOUT * ((t >= 0) ? 1 : 1 - t);
                deadline = System.nanoTime() + parkTime - TIMEOUT_SLOP;
            }
            else
                prevctl = parkTime = deadline = 0L;
            Thread wt = Thread.currentThread();
            U.putObject(wt, PARKBLOCKER, this);   // 模拟 LockSupport。其实就是设置阻塞对象，表示当前线程阻塞在哪个对象上
            w.parker = wt; // 设置当前workqueue阻塞在哪个线程上
            if (w.scanState < 0 && ctl == c)      // 在执行阻塞之前，再次检测状态位：当前worker必须处于空闲状态且ctl没有被改变过
                U.park(false, parkTime);
            // 被唤醒后，清空标识字段
            U.putOrderedObject(w, QPARKER, null);
            U.putObject(wt, PARKBLOCKER, null);
            // 被唤醒后，扫描状态被设置>0，说明处于活跃状态，直接break
            if (w.scanState >= 0)
                break;
            if (parkTime != 0L && // 设置了阻塞时间
                ctl == c && // ctl没有改变过
                deadline - System.nanoTime() <= 0L && // 超时了
                U.compareAndSwapLong(this, CTL, c, prevctl)) // 收缩线程池
                return false;                    
        }
    }
    return true;
}

// FJP关闭线程池方法
public void shutdown() {
    tryTerminate(false, true);
}
// 实际关闭方法。enable指明如果线程池状态处于活跃状态时，能不能修改状态。
private boolean tryTerminate(boolean now, boolean enable) {
    int rs;
    if (this == common)                       // common公用线程池不允许被关闭
        return false;
    if ((rs = runState) >= 0) {               // 线程池处于活跃状态
        if (!enable) // 如果enable为false，直接退出
            return false;
        rs = lockRunState();                  // 进入 SHUTDOWN 阶段
        unlockRunState(rs, (rs & ~RSLOCK) | SHUTDOWN); // 去除RSLOCK位，并加上SHUTDOWN标志位
    }
    // 此时标志位SHUTDOWN已经设置，那么通过之前的描述得知，此时线程池不会再接收新的任务
    if ((rs & STOP) == 0) { // 此时，线程池状态处于SHUTDOWN状态，没有设置STOP标志位
        if (!now) {                           // 如果没有设置立即无条件结束线程池，那么需要检测一下静默状态，看看是否所有线程都是空闲的
            for (long oldSum = 0L;;) {        // 重复检测，直到FJP稳定
                WorkQueue[] ws; WorkQueue w; int m, b; long c;
                long checkSum = ctl; // 校验和变量默认等于最新的ctl值
                if ((int)(checkSum >> AC_SHIFT) + (config & SMASK) > 0) // 计算ac值
                    return false;             // ac大于0，说明仍然有活跃线程，直接返回即可
                if ((ws = workQueues) == null || (m = ws.length - 1) <= 0)
                    break;                    // wqs都没有创建，那就直接结束呗
                // 遍历所有队列。包括：外部提交队列、内部工作队列（窃取队列）
                for (int i = 0; i <= m; ++i) {
                    if ((w = ws[i]) != null) { // 队列存在
                        if ((b = w.base) != w.top ||  // 队列中有任务
                            w.scanState >= 0 || // 处于工作状态
                            w.currentSteal != null) { // 正在执行任务
                            // 唤醒INACTIVE线程，尽快完成工作
                            tryRelease(c = ctl, ws[m & (int)c], AC_UNIT);
                            return false; // 当前FJP还有任务在执行，当前线程先返回
                        }
                        // 计算校验和
                        checkSum += b;
                        // 取wqs偶数位，设置qlock=-1。禁用外部队列
                        if ((i & 1) == 0)
                            w.qlock = -1;     // try to disable external
                    }
                }
                // 检测FJP是否处于稳定状态，也即没有任务出入，遍历两次。
                if (oldSum == (oldSum = checkSum))
                    break;
            }
        }
        // 当前线程，发现队列处于稳定状态，且已经没有任何任务可执行。直接将状态变为STOP
        if ((runState & STOP) == 0) {
            rs = lockRunState();              // enter STOP phase
            unlockRunState(rs, (rs & ~RSLOCK) | STOP);
        }
    }
	// 此时进入STOP阶段，那么开始帮忙转变状态为terminate
    int pass = 0;                             // 通过三个步骤来逐步帮助terminate线程池
    for (long oldSum = 0L;;) {                // 循环直到完成或者稳定
        WorkQueue[] ws; WorkQueue w; ForkJoinWorkerThread wt; int m;
        long checkSum = ctl;
        // 这是不是最终状态
        if ((short)(checkSum >>> TC_SHIFT) + (config & SMASK) <= 0 || // 总线程数为0，也即FJP中没有活动和非活动线程，也即所有的线程都没了
            (ws = workQueues) == null || (m = ws.length - 1) <= 0) { // 队列不存在
            // 当前线程直接转变状态即可
            if ((runState & TERMINATED) == 0) {
                rs = lockRunState();          // done
                // 将状态改为最终状态：TERMINATED
                unlockRunState(rs, (rs & ~RSLOCK) | TERMINATED);
                // 从这里立刻得知：awaitTermination，等待线程池终结是通过this对象进行阻塞
                synchronized (this) { notifyAll(); } // for awaitTermination
            }
            break;
        }
        // 有线程且队列存在。那么，遍历每一个wq。只处理wq存在的队列。STOP状态含义：工作线程停止工作不管队列中是否还有任务。
        for (int i = 0; i <= m; ++i) {
            if ((w = ws[i]) != null) {
                checkSum += w.base; // 计算校验和，判定队列处于稳定状态，不能放也不能取。
                w.qlock = -1;                 // 直接禁用掉所有的队列wq。此时，不管队列里面是否有任务，都不在执行，详情请看scan方法
                // pass为0时，为第一次进入，所以不会执行下面的语句
                if (pass > 0) {
                    w.cancelAll();            // 将队列中剩余的任务都清空              
                    if (pass > 1 && (wt = w.owner) != null) { // 只有内部工作队列
                          // 如果队列中线程还处于运行状态，那么将其中断
                        if (!wt.isInterrupted()) {
                            try {             // unblock join
                                wt.interrupt();
                            } catch (Throwable ignore) {
                            }
                        }
                        // 如果线程处于INACTIVE状态，那么将其唤醒
                        if (w.scanState < 0)
                            U.unpark(wt);     // wake up
                    }
                }
            }
        }
        // checksum由上面的w.base来决定，如果仍有队列不为空，这时checksum会改变，导致处于不稳定状态，那么重置pass继续循环
        if (checkSum != oldSum) {         
            oldSum = checkSum;
            pass = 0;
        }
        // 到达这里的判断，也即所有的队列都处于稳定状态
        else if (pass > 3 && pass > m)        // 如果当前线程已经经过三次循环，还没有满足第一个判断句，也即总线程数为0，那么看看循环次数是否大于wqs的长度，如果是，那么直接退出，否则继续
            break;
        else if (++pass > 1) {                // 尝试将所有在等待栈中的线程全部唤醒
            long c; int j = 0, sp;           
            while (j++ <= m && (sp = (int)(c = ctl)) != 0)
                tryRelease(c, ws[sp & m], AC_UNIT);
        }
    }
    return true;
}
// 等待FJP线程状态变为TERMINATED
public boolean awaitTermination(long timeout, TimeUnit unit)
    throws InterruptedException {
    // 响应中断
    if (Thread.interrupted())
        throw new InterruptedException();
     // common线程池是不能够被关闭的，所以直接调用awaitQuiescence，然后返回false
    if (this == common) {
        awaitQuiescence(timeout, unit);
        return false;
    }
    long nanos = unit.toNanos(timeout);
    if (isTerminated()) // 已经终结了，那么直接返回true
        return true;
    // 不等待，直接返回false
    if (nanos <= 0L)
        return false;
    // 计算等待的截止时间
    long deadline = System.nanoTime() + nanos;
    // 阻塞在this FJP对象的监视器锁中，直到状态变为TERMINATED然后被唤醒
    synchronized (this) {
        for (;;) {
            if (isTerminated()) // 要么关闭
                return true;
            if (nanos <= 0L) // 要么超时
                return false;
            long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
            wait(millis > 0L ? millis : 1L); // 等待即可
            nanos = deadline - System.nanoTime();
        }
    }
}

// FJT的方法，由FJP的工作线程调用
volatile int status; // 当前FJT的运行状态
static final int DONE_MASK   = 0xf0000000;  // 取完成位的掩码
static final int NORMAL      = 0xf0000000;  // 正常状态
static final int CANCELLED   = 0xc0000000;  // 被取消了
static final int EXCEPTIONAL = 0x80000000;  // 发生了异常
static final int SIGNAL      = 0x00010000;  // 需要唤醒
static final int SMASK       = 0x0000ffff;  // 取低16位的掩码
final int doExec() {
    int s; boolean completed;
    // 默认状态就是0，新建状态
    if ((s = status) >= 0) {
        try {
            completed = exec(); // 子类需要实现该方法完成调用
        } catch (Throwable rex) {
            return setExceptionalCompletion(rex);
        }
        // 如果执行完成，那么设置状态为NORMAL，表示正常完成
        if (completed)
            s = setCompletion(NORMAL);
    }
    return s;
}

// FJT的fork方法，将任务放入FJP中执行
public final ForkJoinTask<V> fork() {
    Thread t;
    if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
        // 如果是内部工作线程，那么直接将其放入自己的队列中
        ((ForkJoinWorkerThread)t).workQueue.push(this);
    else
        // 外部线程就调用externalPush，其实就是调用Common线程池来执行
        ForkJoinPool.common.externalPush(this);
    return this;
}

// FJP用于内部工作线程往自己的队列存放任务
final void push(ForkJoinTask<?> task) {
    ForkJoinTask<?>[] a; ForkJoinPool p;
    int b = base, s = top, n;
    if ((a = array) != null) {    // ignore if queue removed
        int m = a.length - 1;     // fenced write for task visibility
        // 获取top变量的偏移量，然后放入task对象
        U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task);
        // 对top值加1
        U.putOrderedInt(this, QTOP, s + 1);
        // 使用putOrdered保证了STORESTORE语义
        if ((n = s - b) <= 1) { // n为top引用和base引用中间的有效任务数
            // 为何这里要小于等于1才唤醒等待线程，只要这里大于1了，那么必然已经都唤醒了
            if ((p = pool) != null)
                p.signalWork(p.workQueues, this);
        }
        else if (n >= m)  // 满了扩容
            growArray();
    }
}

// 等待当前任务执行完完成
public final V join() {
    int s;
    // 等待过程中发现任务执行出了异常，然后调用reportException抛出异常
    if ((s = doJoin() & DONE_MASK) != NORMAL)
        reportException(s);
    // 正常完成就拿结果即可
    return getRawResult();
}

// CAS上把锁
private int lockRunState() {
    int rs;
    return ((((rs = runState) & RSLOCK) != 0 ||
             !U.compareAndSwapInt(this, RUNSTATE, rs, rs |= RSLOCK)) ?
            awaitRunStateLock() : rs);
}
// 等待获取锁
private int awaitRunStateLock() {
    Object lock;
    boolean wasInterrupted = false;
    for (int spins = SPINS, r = 0, rs, ns;;) {
        // 锁已经被释放，那么可以去CAS竞争锁
        if (((rs = runState) & RSLOCK) == 0) {
            if (U.compareAndSwapInt(this, RUNSTATE, rs, ns = rs | RSLOCK)) {
                if (wasInterrupted) {
                    try {
                        Thread.currentThread().interrupt();
                    } catch (SecurityException ignore) {
                    }
                }
                return ns;
            }
        }
        else if (r == 0) // 初始化随机数
            r = ThreadLocalRandom.nextSecondarySeed();
        else if (spins > 0) { // 随机，减少自旋次数
            r ^= r << 6; r ^= r >>> 21; r ^= r << 7; // 异或随机数
            if (r >= 0)
                --spins;
        }
        else if ((rs & STARTED) == 0 || (lock = stealCounter) == null)
            Thread.yield();   // 由于当前rs的STARTED状态位为0，代表了，当前FJP没有在运行了，那么没有必要再去睡眠了，因为这个状态维持时间会非常短
        // 光是睡眠不行，需要有人唤醒，所以这里必须置位 RSIGNAL 唤醒位，提示另外的线程需要唤醒它
        else if (U.compareAndSwapInt(this, RUNSTATE, rs, rs | RSIGNAL)) {
            synchronized (lock) {
                if ((runState & RSIGNAL) != 0) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        if (!(Thread.currentThread() instanceof
                              ForkJoinWorkerThread))
                            wasInterrupted = true;
                    }
                }
                else
                    lock.notifyAll();
            }
        }
    }
}
// 解锁
private void unlockRunState(int oldRunState, int newRunState) {
    if (!U.compareAndSwapInt(this, RUNSTATE, oldRunState, newRunState)) {
        Object lock = stealCounter;
        runState = newRunState;              // clears RSIGNAL bit
        if (lock != null)
            synchronized (lock) { lock.notifyAll(); }
    }
}
```

