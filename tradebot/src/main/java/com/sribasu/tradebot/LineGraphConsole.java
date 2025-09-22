package com.sribasu.tradebot;

public class LineGraphConsole {
	private static final int HEIGHT = 10; // Max height of graph
private static final String CLEAR_SCREEN = "\033[H\033[2J";
private static final String MOVE_CURSOR_TOP = "\033[H";

// Call this method repeatedly to refresh the graph
public static void printGraph(int[] data) {
    System.out.print(CLEAR_SCREEN); // Clear screen
    System.out.print(MOVE_CURSOR_TOP); // Move cursor to top

    for (int row = HEIGHT; row >= 0; row--) {
        for (int val : data) {
            if (val >= row) {
                System.out.print("^ ");
            } else {
                System.out.print("  ");
            }
        }
        System.out.println();
    }
}

// Overloaded method for 2D arrays
public static void printGraph(int[][] data) {
    System.out.print(CLEAR_SCREEN);
    System.out.print(MOVE_CURSOR_TOP);

    for (int row = HEIGHT; row >= 0; row--) {
        for (int[] series : data) {
            for (int val : series) {
                if (val >= row) {
                    System.out.print("█ ");
                } else {
                    System.out.print("  ");
                }
            }
            System.out.print("   "); // spacing between series
        }
        System.out.println();
    }
}

// Example usage
public static void show(String[] args) throws InterruptedException {
    int[] values = {1, 3, 5, 7, 9, 6, 4, 2, 0};
    for (int i = 0; i < 20; i++) {
        // Simulate changing data
        for (int j = 0; j < values.length-1; j++) {
            values[j] = values[j+1];
        }
        values[values.length-1]=(int) (Math.random() * HEIGHT);
        printGraph(values);
        Thread.sleep(500);
    }
}}