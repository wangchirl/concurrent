package com.shadow.concurrent.threadlocal;

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

		System.out.println(random.nextInt(100));
		System.out.println(localRandom.nextInt(100));

	}
}
