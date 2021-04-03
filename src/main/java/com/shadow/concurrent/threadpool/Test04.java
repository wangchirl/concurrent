package com.shadow.concurrent.threadpool;

import com.shadow.utils.ConsolePrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author shadow
 * @create 2020-09-20
 * @description
 */
public class Test04 {
	public static void main(String[] args) {

		List<Integer> nums = new ArrayList<>(10000);
		Random random = new Random();
		for (int i = 0; i < 10000; i++) {
			nums.add(random.nextInt(100000));
		}
		// normal
		long start = System.currentTimeMillis();
		nums.forEach( Test04::isPrime);
		ConsolePrinter.printlnCyan(System.currentTimeMillis() - start);

		// normal stream
		start = System.currentTimeMillis();
		nums.stream().forEach( Test04::isPrime);
		ConsolePrinter.printlnCyan(System.currentTimeMillis() - start);

		// parallel
		start = System.currentTimeMillis();
		nums.parallelStream().forEach(Test04::isPrime);
		ConsolePrinter.printlnCyan(System.currentTimeMillis() - start);

	}

	public static boolean isPrime(int num) {
		for (int i = 2; i < num / 2; i++) {
			if(num % i == 0) return false;
		}
		return true;
	}
}
