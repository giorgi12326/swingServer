package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static org.example.SimpleMove.*;

public class GrapplingHead extends Shootable{
    public static List<Pair<Integer>> edges =  new ArrayList<>();

    float r = 0.3f;

    public GrapplingHead(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        nodes = getNodes();
        moveSpeed = 40f;
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

        edges.add(new Pair<>(8,9));
        edges.add(new Pair<>(9,10));
        edges.add(new Pair<>(10,11));
        edges.add(new Pair<>(11,8));

        edges.add(new Pair<>(12,13));
        edges.add(new Pair<>(13,14));
        edges.add(new Pair<>(14,15));
        edges.add(new Pair<>(15,12));

        edges.add(new Pair<>(16,17));
        edges.add(new Pair<>(17,18));
        edges.add(new Pair<>(18,19));
        edges.add(new Pair<>(19,16));

        edges.add(new Pair<>(20,21));
        edges.add(new Pair<>(21,22));
        edges.add(new Pair<>(22,23));
        edges.add(new Pair<>(23,20));

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

        edges.add(new Pair<>(12,20));
        edges.add(new Pair<>(13,16));
        edges.add(new Pair<>(14,19));
        edges.add(new Pair<>(15,23));
        //second hand

        edges.add(new Pair<>(24,25));
        edges.add(new Pair<>(25,26));
        edges.add(new Pair<>(26,27));
        edges.add(new Pair<>(27,24));

        edges.add(new Pair<>(28,29));
        edges.add(new Pair<>(29,30));
        edges.add(new Pair<>(30,31));
        edges.add(new Pair<>(31,28));

        edges.add(new Pair<>(32,33));
        edges.add(new Pair<>(33,34));
        edges.add(new Pair<>(34,35));
        edges.add(new Pair<>(35,32));

        edges.add(new Pair<>(36,37));
        edges.add(new Pair<>(37,38));
        edges.add(new Pair<>(38,39));
        edges.add(new Pair<>(39,36));

        edges.add(new Pair<>(24,28));
        edges.add(new Pair<>(25,29));
        edges.add(new Pair<>(26,30));
        edges.add(new Pair<>(27,31));

        edges.add(new Pair<>(28,32));
        edges.add(new Pair<>(29,33));
        edges.add(new Pair<>(30,34));
        edges.add(new Pair<>(31,35));

        edges.add(new Pair<>(32,36));
        edges.add(new Pair<>(33,37));
        edges.add(new Pair<>(34,38));
        edges.add(new Pair<>(35,39));

        edges.add(new Pair<>(36,17));
        edges.add(new Pair<>(37,21));
        edges.add(new Pair<>(38,22));
        edges.add(new Pair<>(39,18));
        //third hand

        edges.add(new Pair<>(40,41));
        edges.add(new Pair<>(41,42));
        edges.add(new Pair<>(42,43));
        edges.add(new Pair<>(43,40));

        edges.add(new Pair<>(44,45));
        edges.add(new Pair<>(45,46));
        edges.add(new Pair<>(46,47));
        edges.add(new Pair<>(47,44));

        edges.add(new Pair<>(48,49));
        edges.add(new Pair<>(49,50));
        edges.add(new Pair<>(50,51));
        edges.add(new Pair<>(51,48));

        edges.add(new Pair<>(52,53));
        edges.add(new Pair<>(53,54));
        edges.add(new Pair<>(54,55));
        edges.add(new Pair<>(55,52));

        edges.add(new Pair<>(40,44));
        edges.add(new Pair<>(41,45));
        edges.add(new Pair<>(42,46));
        edges.add(new Pair<>(43,47));

        edges.add(new Pair<>(44,48));
        edges.add(new Pair<>(45,49));
        edges.add(new Pair<>(46,50));
        edges.add(new Pair<>(47,51));

        edges.add(new Pair<>(48,52));
        edges.add(new Pair<>(49,53));
        edges.add(new Pair<>(50,54));
        edges.add(new Pair<>(51,55));

        edges.add(new Pair<>(52,19));
        edges.add(new Pair<>(53,18));
        edges.add(new Pair<>(54,22));
        edges.add(new Pair<>(55,23));

//
        edges.add(new Pair<>(12,20));
        edges.add(new Pair<>(13,16));
        edges.add(new Pair<>(14,19));
        edges.add(new Pair<>(15,23));



    }
    public Triple[] getNodes() {
        float dx = (float) Math.cos(Math.PI / 6) * r;
        float dy = (float) Math.sin(Math.PI / 6) * r;
        return new Triple[]{
                new Triple(x + dx,y + dy, z),
                new Triple(x + dx - 0.1f,y + dy, z),
                new Triple(x + dx - 0.1f,y + dy - 0.1f, z),
                new Triple(x + dx,y + dy - 0.1f, z),

                new Triple(x + dx + 0.05f,y + dy + 0.05f, z - 0.05f),
                new Triple(x + dx - 0.05f,y + dy + 0.05f, z - 0.05f),
                new Triple(x + dx - 0.05f,y + dy - 0.05f, z - 0.05f),
                new Triple(x + dx + 0.05f,y + dy - 0.05f, z - 0.05f),

                new Triple(x + dx + 0.05f,y + dy + 0.05f, z - 0.15f),
                new Triple(x + dx - 0.05f,y + dy + 0.05f, z - 0.15f),
                new Triple(x + dx - 0.05f,y + dy - 0.05f, z - 0.15f),
                new Triple(x + dx + 0.05f,y + dy - 0.05f, z - 0.15f),

                new Triple(x + dx - 0.1f,y + dy - 0.05f, z - 0.25f),
                new Triple(x + dx - 0.2f,y + dy - 0.05f, z - 0.25f),
                new Triple(x + dx - 0.2f,y + dy - 0.15f, z - 0.25f),
                new Triple(x + dx - 0.1f,y + dy - 0.15f, z - 0.25f),

                //center
                new Triple(x + 0.05f,y + 0.05f, z - 0.4f),
                new Triple(x - 0.05f,y + 0.05f, z - 0.4f),
                new Triple(x - 0.05f,y - 0.05f, z - 0.4f),
                new Triple(x + 0.05f,y - 0.05f, z - 0.4f),

                new Triple(x + 0.05f,y + 0.05f, z - 0.5f),
                new Triple(x - 0.05f,y + 0.05f, z - 0.5f),
                new Triple(x - 0.05f,y - 0.05f, z - 0.5f),
                new Triple(x + 0.05f,y - 0.05f, z - 0.5f),
                //center

                new Triple(x - dx + 0.1f,y + dy, z),//24
                new Triple(x - dx,y + dy, z),
                new Triple(x - dx,y + dy - 0.1f, z),
                new Triple(x - dx + 0.1f,y + dy - 0.1f, z),

                new Triple(x - dx + 0.05f,y + dy + 0.05f, z - 0.05f),
                new Triple(x - dx - 0.05f,y + dy + 0.05f, z - 0.05f),
                new Triple(x - dx - 0.05f,y + dy - 0.05f, z - 0.05f),
                new Triple(x - dx + 0.05f,y + dy - 0.05f, z - 0.05f),

                new Triple(x - dx + 0.05f,y + dy + 0.05f, z - 0.15f),
                new Triple(x - dx - 0.05f,y + dy + 0.05f, z - 0.15f),
                new Triple(x - dx - 0.05f,y + dy - 0.05f, z - 0.15f),
                new Triple(x - dx + 0.05f,y + dy - 0.05f, z - 0.15f),

                new Triple(x - dx + 0.2f,y + dy - 0.05f, z - 0.25f),
                new Triple(x - dx + 0.1f,y + dy - 0.05f, z - 0.25f),
                new Triple(x - dx + 0.1f,y + dy - 0.15f, z - 0.25f),
                new Triple(x - dx + 0.2f,y + dy - 0.15f, z - 0.25f),
                //third

                new Triple(x + 0.05f,y - r + 0.1f, z),//24
                new Triple(x - 0.05f,y - r + 0.1f, z),
                new Triple(x - 0.05f,y - r, z),
                new Triple(x + 0.05f,y - r, z),

                new Triple(x + 0.05f,y - r + 0.05f, z - 0.05f),
                new Triple(x - 0.05f,y - r + 0.05f, z - 0.05f),
                new Triple(x - 0.05f,y - r - 0.05f, z - 0.05f),
                new Triple(x + 0.05f,y - r - 0.05f, z - 0.05f),

                new Triple(x + 0.05f,y - r + 0.05f, z - 0.15f),
                new Triple(x - 0.05f,y - r + 0.05f, z - 0.15f),
                new Triple(x - 0.05f,y - r - 0.05f, z - 0.15f),
                new Triple(x + 0.05f,y - r - 0.05f, z - 0.15f),

                new Triple(x + 0.05f,y - r + 0.15f, z - 0.25f),
                new Triple(x - 0.05f,y - r + 0.15f, z - 0.25f),
                new Triple(x - 0.05f,y - r + 0.05f, z - 0.25f),
                new Triple(x + 0.05f,y - r + 0.05f, z - 0.25f),

        };
    }

    @Override
    public List<Pair<Integer>> getStaticEdges() {
        return edges;
    }
}
