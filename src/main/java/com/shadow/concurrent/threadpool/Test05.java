package com.shadow.concurrent.threadpool;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * @author shadow
 * @create 2020-09-22
 * @description
 */
public class Test05 {
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		ExecutorService executorService = new ThreadPoolExecutor(10,20,10,TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(10),new ThreadPoolExecutor.AbortPolicy());

		CompletionService<Integer> completionService = new ExecutorCompletionService(executorService);

		IntStream.range(0,10).forEach(i ->{
			completionService.submit(() ->{
				long time = (long) (Math.random() * 1000);
				TimeUnit.MILLISECONDS.sleep(time);
				ConsolePrinter.printlnCyan(Thread.currentThread().getName() + ":" + time);
				return i * i;
			});
		});

		for (int i = 0; i < 10; i++) {
			ConsolePrinter.printlnCyan(completionService.take().get());
		}

		executorService.shutdown();

	}
}
