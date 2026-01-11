package org.example;

import java.awt.*;
import java.awt.geom.Line2D;


public abstract class Projectable {
    float x;
    float y;
    float z;

    Triple[] nodes;

    Pair<Float> rotation = new Pair<>(0f,0f);

    protected abstract Triple[] getNodes();


}
