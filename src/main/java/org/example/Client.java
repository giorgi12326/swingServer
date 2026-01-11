package org.example;

import java.net.InetAddress;

public class Client {
    InetAddress ip;
    public int port;

    Triple sum = new Triple(0f,0f,0f);
    Triple cameraCoords = new Triple(0f,0f,0f);
    Pair<Float> cameraRotation = new Pair<>(0f,0f);
    BulletHead heldBullet = new BulletHead();
    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    boolean swinging;
    boolean grapplingEquipped;
    boolean inAir;

    float speedX = 0f;
    float speedY = 0f;
    float speedZ = 0f;


    public Client(InetAddress address, int port) {
        this.ip = address;
        this.port = port;
    }
}
