package org.example;

import java.awt.*;
import java.awt.geom.Line2D;


public class Ray {
    Triple position;
    Triple deltaDirection;
    Pair<Float> direction;
    float size;

    public Ray(Triple position, Pair<Float> direction, float size) {
        this.position = position;
        this.direction = direction;
        this.size = size;
    }

    boolean rayIntersectsCube(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ
    ) {
        float tMin = (minX - position.x) / Ray.this.deltaDirection.x;
        float tMax = (maxX - position.x) / Ray.this.deltaDirection.x;
        if (tMin > tMax) { float tmp = tMin; tMin = tMax; tMax = tmp; }

        float tyMin = (minY - position.y) / deltaDirection.y;
        float tyMax = (maxY - position.y) / deltaDirection.y;
        if (tyMin > tyMax) { float tmp = tyMin; tyMin = tyMax; tyMax = tmp; }

        if (tMin > tyMax || tyMin > tMax) return false;

        if (tyMin > tMin) tMin = tyMin;
        if (tyMax < tMax) tMax = tyMax;

        float tzMin = (minZ - position.z) / deltaDirection.z;
        float tzMax = (maxZ - position.z) / deltaDirection.z;
        if (tzMin > tzMax) { float tmp = tzMin; tzMin = tzMax; tzMax = tmp; }

        if (tMin > tzMax || tzMin > tMax) return false;

        return tMax >= 0;
    }

}
