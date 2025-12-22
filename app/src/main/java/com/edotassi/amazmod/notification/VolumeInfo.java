package com.edotassi.amazmod.notification;

public class VolumeInfo {
    final int current;
    final int max;

    VolumeInfo(int current, int max) {
        this.current = current;
        this.max = max;
    }

    public int getCurrent() {
        return current;
    }

    public int getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "VolumeInfo{" +
                "current=" + current +
                ", max=" + max +
                '}';
    }
}
