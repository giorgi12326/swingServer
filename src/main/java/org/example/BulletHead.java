package org.example;

import java.util.ArrayList;
import java.util.List;

public class BulletHead extends Shootable {
    public static List<Pair<Integer>> edges =  new ArrayList<>();

    public BulletHead() {
        nodes = getNodes();
        moveSpeed = 20f;
    }

    static{
        edges.add(new Pair<>(0,1));
        edges.add(new Pair<>(1,2));
        edges.add(new Pair<>(2,3));
        edges.add(new Pair<>(3,0));

        edges.add(new Pair<>(4,5));
        edges.add(new Pair<>(5,6));
        edges.add(new Pair<>(6,7));
        edges.add(new Pair<>(7,4));

        edges.add(new Pair<>(0,4));
        edges.add(new Pair<>(1,5));
        edges.add(new Pair<>(2,6));
        edges.add(new Pair<>(3,7));

        edges.add(new Pair<>(4,8));
        edges.add(new Pair<>(5,8));
        edges.add(new Pair<>(6,8));
        edges.add(new Pair<>(7,8));

    }
    public Triple[] getNodes() {
        float dx = (float) Math.cos(Math.PI / 6) * r;
        float dy = (float) Math.sin(Math.PI / 6) * r;
        return new Triple[]{
                new Triple(x + dx + 0.04f,y + dy + 0.04f, z),
                new Triple(x + dx - 0.04f,y + dy + 0.04f, z),
                new Triple(x + dx - 0.04f,y + dy - 0.04f, z),
                new Triple(x + dx + 0.04f,y + dy - 0.04f, z),

                new Triple(x + dx + 0.04f,y + dy + 0.04f, z + 0.16f),
                new Triple(x + dx - 0.04f,y + dy + 0.04f, z + 0.16f),
                new Triple(x + dx - 0.04f,y + dy - 0.04f, z + 0.16f),
                new Triple(x + dx + 0.04f,y + dy - 0.04f, z + 0.16f),

                new Triple(x + dx,y + dy, z + 0.2f),
        };
    }

    @Override
    public List<Pair<Integer>> getStaticEdges() {
        return edges;
    }


}
