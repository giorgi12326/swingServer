package org.example;

import org.example.powerUp.PowerUp;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import static org.example.SenderAndReceiver.broadCastState;

public class SimpleMove {
    final long TICK_RATE = 60;
    final long TICK_DURATION_MS = 1000 / TICK_RATE; // ≈16 ms per tick

    public static Set<Client> clients = new HashSet<>();
    public static ByteBuffer senderBuffer = ByteBuffer.allocate(4096); // 4 bytes per float * 3

    public static  boolean deathCubeSpawnMode = false;
    public static float deltaTime = 1f;
    long lastTime = System.nanoTime();

    long deathCubeLastSpawnTime = System.currentTimeMillis();

    public static List<Cube> cubes;
    public static List<DeathCube> deathCubes = new ArrayList<>();
    List<Triple> floor = new ArrayList<>();
    public static List<BulletHead> bullets = new ArrayList<>();
    List<PowerUp> powerUps = new ArrayList<>();

    public static final float GRAVITY = 10f;

    public static DatagramSocket socket;

    public SimpleMove() {
        this.cubes = new ArrayList<>();
        cubes.add(new Cube(0.5f,0.5f, 3.5f,1f));
        cubes.add(new Cube(0.5f,1.5f, 5.5f,1f));
        cubes.add(new Cube(0.5f,2.5f, 6.5f,1f));
        cubes.add(new Cube(0.5f,3.5f, 9.5f,1f));
        cubes.add(new Cube(0.5f,3.5f, 12.5f,1f));
        cubes.add(new Cube(0.5f,3.5f, 15.5f,1f));
        cubes.add(new Cube(0.5f,4.5f, 18.5f,1f));
        cubes.add(new Cube(0.5f,4.5f, 23.5f,1f));
        cubes.add(new Cube(0.5f,4.5f, 30.5f,1f));

        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                floor.add(new Triple(i*1f, 0f, j*1f));
            }
        }
    }

    public void start()  {
        try {
            socket = new DatagramSocket(1234, InetAddress.getByName("0.0.0.0"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Socket Created");

        new Thread(new SenderAndReceiver()).start();

        while(true) {
            long tickStart = System.currentTimeMillis();

            long now = System.currentTimeMillis();
            deltaTime = (now - lastTime) / 1000f;
            lastTime = now;

            for (Client c : clients) {
                boolean w, a, s, d, space, one, two, three, leftClick, rightClick;
                float rotationX, rotationY;

                synchronized (c.mutex) {
                    w = c.latestInput.w;
                    a = c.latestInput.a;
                    s = c.latestInput.s;
                    d = c.latestInput.d;
                    space = c.latestInput.space;
                    one = c.latestInput.one;
                    two = c.latestInput.two;
                    three = c.latestInput.three;
                    leftClick = c.latestInput.leftClick;
                    rightClick = c.latestInput.rightClick;
                    rotationX = c.latestInput.rotationX;
                    rotationY = c.latestInput.rotationY;
                }
                useReceivedData(c, rotationX, rotationY, w, a, s, d, space, one, two, three, leftClick, rightClick);
            }

            bullets.forEach(Shootable::update);
            handleDeletionMarks();
            handleBulletHittingSomething();
            broadCastState();

            try {
                Thread.sleep((int)(Math.max(0, TICK_DURATION_MS - (System.currentTimeMillis() - tickStart))));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void useReceivedData(Client client, float rotationX, float rotationY, boolean w, boolean a, boolean s, boolean d, boolean space, boolean one, boolean two, boolean three, boolean leftClick, boolean rightClick) {
        client.handleIfDead();
        if(client.isDead) return;

        updatePerClient(client);

        client.cameraRotation.x = rotationX;
        client.cameraRotation.y = rotationY;

        if(w) {
            Triple triple = client.moveForward();
            client.speedX += triple.x;
            client.speedZ += triple.z;
        }
        if(a) {
            Triple triple = client.moveLeft();
            client.speedX += triple.x;
            client.speedZ += triple.z;
        }
        if(s) {
            Triple triple = client.moveBackward();
            client.speedX += triple.x;
            client.speedZ += triple.z;
        }
        if(d) {
            Triple triple = client.moveRight();
            client.speedX += triple.x;
            client.speedZ += triple.z;
        }

        if(space && !client.inAir) {
            client.sum.y = 8f*deltaTime;
            client.inAir = true;
        }

        if(leftClick) {
            if(client.grapplingEquipped && !client.grapplingHead.shot)
                client.grapplingHead.prepareShootableForFlying(client);
            else {
                if(client.heldBullet != null) {
                    BulletHead.generatePreparedBulletForAndAddToList(client);
                    client.shotBulletHandleState();
                }
            }
        }

        if(one){
            client.grapplingEquipped = false;
            client.swinging = false;
            client.grapplingHead.shot = false;
            client.grapplingHead.flying = false;
        }

        if(two){
            client.grapplingEquipped = true;
        }
    }


    private void updatePerClient(Client client) {
        if(client.swinging)
            client.swingAround();
        else
            client.moveCharacter();//? in else?

        if(client.heldBullet == null && System.currentTimeMillis() - client.bulletShotLastTime > 333){
            client.heldBullet = new BulletHead();
        }

        client.grapplingHead.update();
        if(client.grapplingHead.shot)
            for(Cube cube : cubes) {
                if(cube.isPointInCube(client.grapplingHead.getNodes()[17])) {
                    client.swinging = true;
                    client.grapplingHead.flying = false;
                    client.anchor = new Triple(cube.x + cube.size / 2f, cube.y + cube.size / 2f, cube.z + cube.size / 2f);
                }
            }

        client.hitbox.x = client.cameraCoords.x;
        client.hitbox.y = client.cameraCoords.y;
        client.hitbox.z = client.cameraCoords.z;

        client.speedX = 0;
        client.speedY = 0;
        client.speedZ = 0;

    }

    private void handleBulletHittingSomething() {
        for (BulletHead bullet : bullets) {
            for (int j = deathCubes.size()-1; j >= 0; j--) {
                DeathCube deathCube = deathCubes.get(j);
                if (deathCube.isPointInCube(bullet.getNodes()[8]))
                    deathCubes.remove(j);
            }
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            BulletHead bullet = bullets.get(i);
            for(Cube cube : cubes) {
                if(cube.isPointInCube(bullet.getNodes()[8]))
                    bullets.remove(i);
            }
        }

        Triple temp = new Triple(0f,0f,0f); // one-time allocation
        for (int i = bullets.size() - 1; i >= 0; i--) {
            BulletHead bullet = bullets.get(i);
            temp.x = bullet.getNodes()[8].x;
            temp.y = bullet.getNodes()[8].y;
            temp.z = bullet.getNodes()[8].z;

            for(Client client : clients) {
                if(client.hitbox.isLineIntersectingCube(
                        new Triple(bullet.prevX, bullet.prevY, bullet.prevZ),
                        new Triple(bullet.x, bullet.y, bullet.z))) {
                    client.cameraCoords.y += 10f;
                    client.health -= 50;
                }
            }
        }
    }

    private void handleDeletionMarks() {
        for (int i = deathCubes.size() - 1; i >= 0; i--) {
            DeathCube deathCube = deathCubes.get(i);
            if(deathCube.markedAsDeleted)
                deathCubes.remove(i);
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            BulletHead bullet = bullets.get(i);
            if(bullet.markedAsDeleted)
                bullets.remove(i);
        }
    }

    public static void main(String[] args) {
        SimpleMove game = new SimpleMove();
        game.start();
    }

}
