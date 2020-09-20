package com.shadow.concurrent.threadpool;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

/**
 * @author shadow
 * @create 2020-09-20
 * @description
 */
public class Test02 {

	static int[] nums = new int[1000000];
	static int MAX = 50000;
	static Random r = new Random();

	static {
		for (int i = 0; i < nums.length; i++) {
			nums[i] = r.nextInt(100);
		}
		System.out.println(Arrays.stream(nums).sum());
	}

	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		// ForkJoinPool
		ForkJoinPool forkJoinPool = new ForkJoinPool();
		ReturnForkJoinTask forkJoinTask = new ReturnForkJoinTask(0, nums.length);
		forkJoinPool.execute(forkJoinTask);
		System.out.println(forkJoinTask.get());

		System.out.println("=====================");

		VoidForkJoinTask voidForkJoinTask = new VoidForkJoinTask(0, nums.length);
		forkJoinPool.execute(voidForkJoinTask);
		System.in.read();

	}

	static class VoidForkJoinTask extends RecursiveAction {
		int start,end;
		VoidForkJoinTask(int start,int end) {
			this.end = end;
			this.start = start;
		}

		@Override
		protected void compute() {
			if(end - start <= MAX) {
				long sum = 0L;
				for (int i = start; i < end; i++) {
					sum += nums[i];
				}
				System.out.println("from:" +start + " to:" + end + "=" + sum);
			}else {
				int middle = (end + start) / 2;
				VoidForkJoinTask voidForkJoinTask1 = new VoidForkJoinTask(start, middle);
				VoidForkJoinTask voidForkJoinTask2 = new VoidForkJoinTask(middle, end);
				voidForkJoinTask1.fork();
				voidForkJoinTask2.fork();
			}
		}
	}

	static class ReturnForkJoinTask extends RecursiveTask<Long> {

		int start,end;

		ReturnForkJoinTask(int start,int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		protected Long compute() {
			if(end - start <= MAX) {
				long sum = 0L;
				for (int i = start; i < end; i++) {
					sum += nums[i];
				}
				return sum;
			}else {
				int middle = (end + start)/2;
				ReturnForkJoinTask joinTask1 = new ReturnForkJoinTask(start, middle);
				ReturnForkJoinTask joinTask2 = new ReturnForkJoinTask(middle, end);
				joinTask1.fork();
				joinTask2.fork();
				return joinTask1.join() + joinTask2.join();
			}
		}
	}
}
