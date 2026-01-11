package org.example;

import java.awt.*;

public class DeathCube extends Cube{
    public static final float MOVE_SPEED = 10f;
    Triple dirNormalized;
    boolean markedAsDeleted = false;

    public DeathCube(float x, float y, float z, Triple cameraCoords, float size) {
        super(x, y, z, size);
        this.dirNormalized = new Triple(x,y,z).sub(cameraCoords).normalize();
    }

    @Override
    public void update() {
        x += MOVE_SPEED * SimpleMove.deltaTime *  -dirNormalized.x;
        y += MOVE_SPEED * SimpleMove.deltaTime  *  -dirNormalized.y;
        z += MOVE_SPEED * SimpleMove.deltaTime  *  -dirNormalized.z;
        if(Math.abs(x) > 50f || Math.abs(y) > 50f || Math.abs(z) > 50f)
            markedAsDeleted = true;
    }

}
