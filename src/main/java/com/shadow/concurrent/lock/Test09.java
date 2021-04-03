package com.shadow.concurrent.lock;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

/**
 * @author shadow
 * @create 2020-09-13
 * @description
 */
public class Test09 {

	static MarigePhaser phaser = new MarigePhaser();

	public static void main(String[] args) {

		phaser.bulkRegister(7);

		for (int i = 0; i < 5; i++) {
			new Thread(new Person(String.valueOf(i))).start();
		}

		new Thread(new Person("新娘")).start();
		new Thread(new Person("新郎")).start();

	}


	static class MarigePhaser extends Phaser {
		@Override
		protected boolean onAdvance(int phase, int registeredParties) {
			switch (phase) {
				case 0:
					ConsolePrinter.printlnCyan("所有人到齐了！" + registeredParties);
					return false;
				case 1:
					ConsolePrinter.printlnCyan("所有人吃完了！" + registeredParties);
					return false;
				case 2:
					ConsolePrinter.printlnCyan("所有人离开了！" + registeredParties);
					return false;
				case 3:
					ConsolePrinter.printlnCyan("婚礼结束，新郎新娘入洞房！" + registeredParties);
					return true;
				default:
					return true;

			}
		}
	}

	static class Person implements Runnable {
		String name;

		Person(String name) {
			this.name = name;
		}

		public void arrive() {
			sleep();
			ConsolePrinter.printlnCyan(String.format("%s 到达现场！" , name));
			phaser.arriveAndAwaitAdvance();
		}

		public void eat() {
			sleep();
			ConsolePrinter.printlnCyan(String.format("%s 吃完了！" , name));
			phaser.arriveAndAwaitAdvance();
		}

		public void leave() {
			sleep();
			ConsolePrinter.printlnCyan(String.format("%s 离开现场！" , name));
			phaser.arriveAndAwaitAdvance();
		}

		private void hug() {
			sleep();
			if(name.equals("新娘") || name.equals("新郎")){
				ConsolePrinter.printlnCyan(String.format("%s 进入洞房！" , name));
				phaser.arriveAndAwaitAdvance();
			}else {
				phaser.arriveAndDeregister();
			}
		}

		@Override
		public void run() {
			arrive();

			eat();

			leave();

			hug();
		}
	}

	public static void sleep() {
		try {
			TimeUnit.MILLISECONDS.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
