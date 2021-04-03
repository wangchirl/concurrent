package com.shadow.utils;

import java.util.Arrays;

/**
 * @author shadow
 * @create 2021-02-11
 * @description
 */
public final class ConsolePrinter {

    public static void printlnCyan(Object... s1) {
        System.out.println(ConsoleColors.CYAN_UNDERLINED + Arrays.toString(s1) + ConsoleColors.RESET);
    }

    public static void printlnRed(Object... s1) {
        System.out.println(ConsoleColors.RED_UNDERLINED + Arrays.toString(s1) + ConsoleColors.RESET);
    }

    public static void printlnYellow(Object... s1) {
        System.out.println(ConsoleColors.YELLOW_UNDERLINED + Arrays.toString(s1) + ConsoleColors.RESET);
    }

    public static void printlnPurple(Object... s1) {
        System.out.println(ConsoleColors.PURPLE_UNDERLINED + Arrays.toString(s1) + ConsoleColors.RESET);
    }
}
