package org.example;

import java.awt.*;
import java.awt.geom.Rectangle2D;


public class Triple {
    float x;
    float y;
    float z;

    public Triple(Float x, Float y, Float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Triple(Triple position) {
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public Triple add(Triple other) {
        return new Triple(x + other.x, y + other.y, z + other.z);
    }

    public Triple sub(Triple other) {
        return new Triple(x - other.x, y - other.y, z - other.z);
    }

    @Override
    public String toString() {
        return "Triple{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public Triple normalize() {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return new Triple(0f, 0f, 0f); // avoid divide by zero
        return new Triple(x / len, y / len, z / len);
    }
    public Triple scale(float scale){
        return new Triple(x*scale, y * scale, z * scale);
    }
    public Triple cross(Triple other) {
        float cx = this.y * other.z - this.z * other.y;
        float cy = this.z * other.x - this.x * other.z;
        float cz = this.x * other.y - this.y * other.x;
        return new Triple(cx, cy, cz);
    }
    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
    public Triple rotateXY( Triple center, Pair<Float> rotation) {
        Triple p = sub(center);

        // X rotation
        float cos = (float)Math.cos(-rotation.x);
        float sin = (float)Math.sin(-rotation.x);
        float y = p.y * cos - p.z * sin;
        float z = p.y * sin + p.z * cos;
        p = new Triple(p.x, y, z);

        // Y rotation
        cos = (float)Math.cos(-rotation.y);
        sin = (float)Math.sin(-rotation.y);
        float x = p.x * cos - p.z * sin;
        z = p.x * sin + p.z * cos;

        return new Triple(x, p.y, z).add(center);
    }


}


