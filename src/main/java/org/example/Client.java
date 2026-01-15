package org.example;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Client {
    InetAddress ip;
    public int port;

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
}
