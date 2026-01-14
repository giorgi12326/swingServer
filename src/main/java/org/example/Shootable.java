package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

import static org.example.SimpleMove.deltaTime;

public abstract class Shootable extends Projectable{
    public boolean shot;
    public boolean flying;
    public Triple direction;
    public float r = 0.3f;
    public float moveSpeed = 20f;
    public boolean markAsDeleted;

    float prevX;
    float prevY;
    float prevZ;

    public abstract Triple[] getNodes();

    public abstract List<Pair<Integer>> getStaticEdges();

    public void update() {
        if(shot && flying){
            prevX = x;
            prevY = y;
            prevZ = z;
            x += direction.x * moveSpeed * deltaTime;
            y += direction.y * moveSpeed * deltaTime;
            z += direction.z * moveSpeed * deltaTime;
            if(Math.abs(x) > 800f ||
                    Math.abs(y) > 800f ||
                    Math.abs(z) > 800f)
                markAsDeleted = true;

        }
    }
}
