package com.shadow.concurrent.threadlocal;

import com.shadow.utils.ConsolePrinter;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.DoubleStream;

/**
 * @author shadow
 * @create 2020-09-22
 * @description
 */
public class Test03 {
	public static void main(String[] args) {

		Random random = new Random();

		ThreadLocalRandom localRandom = ThreadLocalRandom.current();

		ConsolePrinter.printlnCyan(random.nextInt(100));
		ConsolePrinter.printlnCyan(localRandom.nextInt(100));

	}
}
