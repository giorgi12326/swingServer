package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class Gun extends Projectable {
    public static List<Pair<Integer>> edges =  new ArrayList<>();

    float x;
    float y;
    float z;

    Triple[] nodes;

    public Gun(float x, float z, float y) {
        this.x = x;
        this.z = z;
        this.y = y;
        nodes = getNodes();
    }
    static{
        edges.add(new Pair<>(0,1));
        edges.add(new Pair<>(1,2));
        edges.add(new Pair<>(2,3));

        edges.add(new Pair<>(4,5));
        edges.add(new Pair<>(5,6));
        edges.add(new Pair<>(6,7));

        edges.add(new Pair<>(0,4));
        edges.add(new Pair<>(1,5));
        edges.add(new Pair<>(2,6));
        edges.add(new Pair<>(3,7));

        edges.add(new Pair<>(4,8));
        edges.add(new Pair<>(5,9));
        edges.add(new Pair<>(6,10));
        edges.add(new Pair<>(7,11));

        edges.add(new Pair<>(8,12));
        edges.add(new Pair<>(9,13));
        edges.add(new Pair<>(10,14));
        edges.add(new Pair<>(11,15));

        edges.add(new Pair<>(12,13));
        edges.add(new Pair<>(13,14));
        edges.add(new Pair<>(14,15));
        edges.add(new Pair<>(15,12));

        edges.add(new Pair<>(8,9));

    }
    @Override
    public Triple[] getNodes() {
        return new Triple[]{
            new Triple(x + 0.04f,y + 0.04f, z + 0.1f),
            new Triple(x - 0.04f,y + 0.04f,z + 0.1f),
            new Triple(x - 0.04f,y - 0.04f,z + 0.1f),
            new Triple(x + 0.04f,y - 0.04f, z + 0.1f),

            new Triple(x + 0.02f,y + 0.02f, z + 0f),
            new Triple(x - 0.02f,y + 0.02f,z + 0f),
            new Triple(x - 0.02f,y - 0.02f,z + 0f),
            new Triple(x + 0.02f,y - 0.02f, z + 0f),

            new Triple(x + 0.01f,y + 0.01f, z -0.2f),
            new Triple(x -0.01f,y + 0.01f, z -0.2f),
            new Triple(x -0.01f,y - 0.01f, z -0.18f),
            new Triple(x + 0.01f,y - 0.01f, z-0.18f),

            new Triple(x + 0.01f,y - 0.1f, z-0.2f),
            new Triple(x -0.01f,y -0.1f, z-0.2f),
            new Triple(x -0.01f,y - 0.1f, z-0.18f),
            new Triple(x + 0.01f,y - 0.1f, z-0.18f),
        };
    }

}
