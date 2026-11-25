package com.gtladd.sceneexample;

import java.util.ArrayList;
import java.util.List;

/** The effects placed with the commands. Client-side state of the current level, render thread only. */
final class Placements {

    record Placement(long id, double x, double y, double z, double radius) {}

    static final List<Placement> ORBS = new ArrayList<>();
    static final List<Placement> BEACONS = new ArrayList<>();
    private static long nextId;

    static void add(List<Placement> list, double x, double y, double z, double radius) {
        list.add(new Placement(nextId++, x, y, z, radius));
    }

    static void clear() {
        ORBS.clear();
        BEACONS.clear();
    }
}
