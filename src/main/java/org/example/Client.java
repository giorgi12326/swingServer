package org.example;

import java.net.InetAddress;

public class Client {
    InetAddress ip;

    Triple sum = new Triple(0f,0f,0f);
    Triple cameraCoords = new Triple(0f,0f,0f);
    Pair<Float> cameraRotation = new Pair<>(0f,0f);
    BulletHead heldBullet = new BulletHead();
    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    boolean swinging;
    boolean grapplingEquipped;
    boolean inAir;

    public Client(InetAddress address) {
        this.ip = address;
    }
}
