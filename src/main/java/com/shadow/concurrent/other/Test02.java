//package com.shadow.concurrent.other;
//
//import java.lang.invoke.MethodHandle;
//import java.lang.invoke.MethodHandles;
//
///**
// * @author shadow
// * @create 2020-09-19
// * @description
// *
// * JDK9 引入的 VarHandle
// */
//public class Test02 {
//
//	private int x;
//
//	static VarHandle handle;
//
//	static {
//		try {
//			handle = MethodHandles.lookup().findVarHandle(Test02.class,"x",int.class);
//		}catch (Exception e){
//			e.printStackTrace();
//		}
//	}
//
//	public static void main(String[] args) {
//		Test02 test02 = new Test02();
//
//		ConsolePrinter.printlnCyan(handle.get(test02));
//		handle.set(test02,10);
//		ConsolePrinter.printlnCyan(test02.x);
//
//		handle.compareAndSet(test02,10,12);
//		ConsolePrinter.printlnCyan(test02.x);
//
//		handle.getAndAdd(test02,10);
//		ConsolePrinter.printlnCyan(test02.x);
//
//
//	}
//}
