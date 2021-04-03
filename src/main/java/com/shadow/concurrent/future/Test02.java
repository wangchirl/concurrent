package com.shadow.concurrent.future;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author shadow
 * @create 2020-09-16
 * @description
 */
public class Test02 {
	public static void main(String[] args) throws InterruptedException {

		String result = CompletableFuture.supplyAsync(() -> "hello ").thenApplyAsync((i) -> i + " world").join();
		ConsolePrinter.printlnCyan(result);

		CompletableFuture.supplyAsync(() -> "everybody")
						.thenAcceptAsync((i) -> ConsolePrinter.printlnCyan(i + " are you ready"));


		result = CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return "hello";
		}).thenCombineAsync(CompletableFuture.supplyAsync(() ->{
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return "world";
		}),(s1,s2) -> s1 + " | " + s2).join();

		ConsolePrinter.printlnCyan(result);


		CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() ->{
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ConsolePrinter.printlnCyan("finished");
		});

		completableFuture.whenComplete((t,action) -> ConsolePrinter.printlnCyan("执行完毕"));

		ConsolePrinter.printlnCyan("main finished");

		TimeUnit.SECONDS.sleep(5);
	}
}
