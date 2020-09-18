package com.shadow.concurrent.lock;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 线程间的通信：
 * 题目：两个线程操作一个初始值为0的变量，四个线程呢？
 *       一个加1，一个减1，交替来10次
 *
 *   1. 高内聚低耦合前提下，线程 操作 资源类
 *   2. 判断/干活/通知  -- 多线程判断使用 while ，不能使用 if
 */
public class Test12 {
    public static void main(String[] args) {

        Cake cake = new Cake();

        new Thread(() -> {
            try {
                for (int i = 1; i <=10 ; i++) {
                    cake.increment();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"A").start();

        new Thread(() -> {
            try {
                for (int i = 1; i <=10 ; i++) {
                    cake.decrement();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"B").start();

//        new Thread(() -> {
//            try {
//                for (int i = 1; i <=10 ; i++) {
//                    cake.increment();
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        },"C").start();
//
//        new Thread(() -> {
//            try {
//                for (int i = 1; i <=10 ; i++) {
//                    cake.decrement();
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        },"D").start();

    }
    static class Cake{

        private int number = 0;

        // Lock版本
        private Lock lock = new ReentrantLock(true);
        private Condition condition1 = lock.newCondition();

        public void increment() throws InterruptedException {
            lock.lock();
            try{
                // 1.判断
                while (number != 0) {
                    condition1.await();
                }
                // 2.干活
                number++;
                System.out.println(Thread.currentThread().getName()+number);
                // 3.通知
                condition1.signalAll();
            }finally {
                lock.unlock();
            }
        }


        public void decrement() throws InterruptedException {
            lock.lock();
            try{
                // 1.判断
                while (number == 0) {
                    condition1.await();
                }
                // 2.干活
                number--;
                System.out.println(Thread.currentThread().getName()+number);
                // 3.通知
                condition1.signalAll();
            }finally {
                lock.unlock();
            }
        }


        // synchronized 版本
        /** public synchronized void increment() throws InterruptedException {
         // 1.判断 多线程下不能使用 if
         while (number != 0) {
         this.wait();
         }
         // 2.干活
         number++;
         System.out.println(Thread.currentThread().getName()+number);
         // 3.通知
         this.notifyAll();
         }

         public synchronized void decrement() throws InterruptedException {
         // 1.判断 多线程下不能使用 if
         while (number == 0) {
         this.wait();
         }
         // 2.干活
         number--;
         System.out.println(Thread.currentThread().getName()+number);
         // 3.通知
         this.notifyAll();
         } */
    }
}
