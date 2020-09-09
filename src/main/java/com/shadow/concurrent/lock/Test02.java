package com.shadow.concurrent.lock;

/**
 * @author shadow
 * @create 2020-09-09
 * @description
 *
 * 传统上，我可以通过synchronized关键字 + wait、notify、notifyAll 来实现多个线程之间的协调与通信，
 * 整个过程都是JVM来帮助我们实现的，开发者无需（也无法）了解底层的实现细节
 *
 * 从JDK5开始，并发包提供了Lock、Condition（await、signal、signalAll）来实现多个线程之间的协调与通信，
 * 整个过程都是由开发者来控制的，而且相比于传统方式，更加灵活，功能也更加强大
 *
 *
 */
public class Test02 {
}
