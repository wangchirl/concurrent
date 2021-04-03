package com.shadow.concurrent.future;

import com.shadow.utils.ConsolePrinter;

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

			ConsolePrinter.printlnCyan("pre ...");
			 Thread.sleep(3000);
			int nextInt = new Random().nextInt(1000);

			ConsolePrinter.printlnCyan("post ...");
			return nextInt;
		};

		FutureTask<Integer> futureTask = new FutureTask<>(callable);

		new Thread(futureTask).start();

		Thread.sleep(1000);
		ConsolePrinter.printlnCyan("main thread...");

		ConsolePrinter.printlnCyan(futureTask.get()); // get()被阻塞
		ConsolePrinter.printlnCyan(futureTask.get(1,TimeUnit.SECONDS)); // 超时异常，但是子线程的任务不受影响，会继续完成

	}
}
