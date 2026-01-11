package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

import static org.example.SimpleMove.SCREEN_HEIGHT;
import static org.example.SimpleMove.deltaTime;

public abstract class Shootable extends Projectable{
    public boolean shot;
    public boolean flying;
    public Triple direction;
    public float r = 0.3f;
    public float moveSpeed = 20f;
    public boolean markAsDeleted;


    public abstract Triple[] getNodes();

    public abstract List<Pair<Integer>> getStaticEdges();

    public void update() {
        if(shot && flying){
            x += direction.x * moveSpeed * deltaTime;
            y += direction.y * moveSpeed * deltaTime;
            z += direction.z * moveSpeed * deltaTime;
            if(Math.abs(x) > 300f ||
                    Math.abs(y) > 300f ||
                    Math.abs(z) > 300f)
                markAsDeleted = true;

        }
    }
}
