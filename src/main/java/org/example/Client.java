package org.example;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static org.example.SimpleMove.deltaTime;

public class Client {
    public boolean isDead;
    int health = 100;
    InetAddress ip;
    public int port;
    long timeOfDeathLocal = 0;

    Triple sum = new Triple(0f,0f,0f);
    Triple cameraCoords = new Triple(0f,0f,0f);
    Pair<Float> cameraRotation = new Pair<>(0f,0f);
    BulletHead heldBullet = new BulletHead();
    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    byte state = 0;
    Triple anchor;
    boolean swinging;
    boolean grapplingEquipped;
    boolean inAir;
    final float moveSpeed = 6f;
    ClientInput latestInput = new ClientInput();
    final Object inputLock = new Object();
    DatagramPacket packet;
    public long lastSecondarySwitch = System.currentTimeMillis();
    long time;

    Cube hitbox = new Cube(0,0,0,1f);

    float speedX = 0f;
    float speedY = 0f;
    float speedZ = 0f;
    long bulletShotLastTime = System.currentTimeMillis();

    public Client(InetAddress inetAddress, int port , ByteBuffer buffer) {
        this.ip = inetAddress;
        this.port = port;
        byte[] arr = buffer.array();
        packet = new DatagramPacket(arr, arr.length, ip, port);

    }

    public void shotBulletHandleState() {
        heldBullet = null; //fix if more bullets
        bulletShotLastTime = System.currentTimeMillis();
    }

    public Triple moveForward() {
        float dx = moveSpeed * (float) Math.sin(cameraRotation.y) * deltaTime;
        float dz = moveSpeed * (float) Math.cos(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public Triple moveBackward() {
        float dx = -moveSpeed * (float) Math.sin(cameraRotation.y) * deltaTime;
        float dz = -moveSpeed * (float) Math.cos(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public Triple moveRight() {
        float dx = moveSpeed * (float) Math.cos(cameraRotation.y) * deltaTime;
        float dz = moveSpeed * (float) -Math.sin(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public Triple moveLeft() {
        float dx = -moveSpeed * (float)Math.cos(cameraRotation.y) * deltaTime;
        float dz = moveSpeed * (float)Math.sin(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public void moveCharacter() {
        float inputX = sum.x;
        float inputZ = sum.z;

        float inputLength = (float)Math.sqrt(inputX*inputX + inputZ*inputZ);
        if(inputLength > 0.001f) {
            inputX /= inputLength;
            inputZ /= inputLength;
        }

        inputX *= moveSpeed;
        inputZ *= moveSpeed;

        final float DRAG_MOVE = 0.1f;
        final float DRAG_IDLE = 30.0f;
        boolean notMoving = sum.x == 0 && sum.z == 0;

        float drag = DRAG_MOVE;

        float dot = speedX * inputX + speedZ * inputZ;

        if ((notMoving && !inAir) || (dot < 0f && !inAir)) {
            drag = DRAG_IDLE;
        }

        speedX -= speedX * drag * deltaTime;
        speedZ -= speedZ * drag * deltaTime;

        speedX += inputX * deltaTime;
        speedZ += inputZ * deltaTime;

        float maxSpeed = 5f;
        float combinedSpeed = (float)Math.sqrt(speedX*speedX + speedZ*speedZ);
        if(combinedSpeed > maxSpeed) {
            speedX = speedX / combinedSpeed * maxSpeed;
            speedZ = speedZ / combinedSpeed * maxSpeed;
        }

        cameraCoords.x += speedX * deltaTime;
        cameraCoords.z += speedZ * deltaTime;
    }

    public void swingAround() {
        Triple toAnchor = anchor.sub(cameraCoords).normalize();
        Triple tangent = toAnchor.normalize();
        cameraCoords = cameraCoords.add(tangent.scale(moveSpeed*2 * deltaTime));
        if(anchor.sub(cameraCoords).length() <= 1f) {
            inAir = true;
            sum.y += 10f;
            swinging = false;
            grapplingEquipped = false;
            grapplingHead.shot = false;
            grapplingHead.flying = false;
        }
    }
}
