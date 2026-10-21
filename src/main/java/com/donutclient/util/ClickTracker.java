package com.donutclient.util;

import java.util.ArrayDeque;
import java.util.Deque;

/** Compte les clics des dernières secondes pour calculer les CPS. */
public final class ClickTracker {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    private ClickTracker() {
    }

    public static void onClick(int button) {
        if (button == 0) LEFT.addLast(System.currentTimeMillis());
        else if (button == 1) RIGHT.addLast(System.currentTimeMillis());
    }

    public static int getLeftCps() {
        return count(LEFT);
    }

    public static int getRightCps() {
        return count(RIGHT);
    }

    private static int count(Deque<Long> clicks) {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
        return clicks.size();
    }
}
