package com.shadow.concurrent.lock;

import com.shadow.utils.ConsolePrinter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author shadow
 * @create 2020-09-13
 * @description
 */
public class Test11 {

	private int count;
	Lock lock = new ReentrantLock();
	ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	Lock readLock = readWriteLock.readLock();
	Lock writeLock = readWriteLock.writeLock();

	// Lock
	public int lockGet() {
		lock.lock();
		try {
			Thread.sleep(1000);
			ConsolePrinter.printlnCyan("lock read...");
			return count;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return -1;
	}

	public void lockSet() {
		lock.lock();
		try {
			++count;
			ConsolePrinter.printlnCyan("lock set");
		} finally {
			lock.unlock();
		}
	}

	// ReadWriteLock
	public int readLockGet() {
		readLock.lock();
		try {
			Thread.sleep(1000);
			ConsolePrinter.printlnCyan("read lock read...");
			return count;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			readLock.unlock();
		}
		return -1;
	}

	public void writeLockSet() {
		writeLock.lock();
		try {
			++count;
			ConsolePrinter.printlnCyan("write lock set...");
		} finally {
			writeLock.unlock();
		}
	}

	public static void main(String[] args) {
		Test11 test11 = new Test11();
		// 读锁、写锁
		for (int i = 1; i <= 18; i++) {
			new Thread(() -> {
				test11.lockGet();
				//test11.readLockGet();
			}, String.valueOf(i)).start();
		}

		for (int i = 1; i <= 2; i++) {
			new Thread(() -> {
				test11.lockSet();
				//test11.writeLockSet();
			}, String.valueOf(i)).start();
		}
	}
}
