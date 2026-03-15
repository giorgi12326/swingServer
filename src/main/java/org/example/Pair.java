package org.example;

public class Pair<T> {
    T x;
    T y;

    public Pair(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public static Triple pairToTripleRotation(Pair<Float> pair) {
        float dx = (float)(Math.cos(pair.x) * Math.sin(pair.y));
        float dy = (float)(Math.sin(pair.x));
        float dz = (float)(Math.cos(pair.x) * Math.cos(pair.y));
        return new Triple(dx, dy, dz);
    }

    public Pair(Pair<T> cameraRotation) {
        this.x = cameraRotation.x;
        this.y = cameraRotation.y;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
    public Pair<T> copy(){
        return new Pair<>(x, y);
    }
}
