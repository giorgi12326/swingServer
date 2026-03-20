package org.example;

import java.util.List;

import static org.example.Pair.pairToTripleRotation;
import static org.example.SimpleMove.deltaTime;

public abstract class Shootable extends Projectable{
    public boolean shot;
    public boolean flying;
    public Triple deltaDirection;
    public float r = 0.3f;
    public float moveSpeed = 20f;
    public boolean markedAsDeleted;

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
            x += deltaDirection.x * moveSpeed * deltaTime;
            y += deltaDirection.y * moveSpeed * deltaTime;
            z += deltaDirection.z * moveSpeed * deltaTime;
            if(Math.abs(x) > 800f ||
                    Math.abs(y) > 800f ||
                    Math.abs(z) > 800f)
                markedAsDeleted = true;

        }
    }

    public void prepareShootableForFlying(Client client) {
        deltaDirection = pairToTripleRotation(client.cameraRotation);
        rotation = new Pair<>(client.cameraRotation.x, client.cameraRotation.y);
        Triple newPosition = new Triple(client.cameraCoords.x +0.3f, client.cameraCoords.y, client.cameraCoords.z + 0.8f).rotateXY(client.cameraCoords, rotation);
        x = newPosition.x;
        y = newPosition.y;
        z = newPosition.z;
        shot = true;
        flying = true;
    }

}
