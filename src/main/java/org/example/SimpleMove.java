package org.example;

import org.example.powerUp.PowerUp;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class SimpleMove {
    final long TICK_RATE = 60;
    final long TICK_DURATION_MS = 1000 / TICK_RATE; // ≈16 ms per tick

    public static Set<Client> clients = new HashSet<>();
    public static ByteBuffer senderBuffer = ByteBuffer.allocate(4096); // 4 bytes per float * 3

    public static  boolean deathCubeSpawnMode = false;
    public static float deltaTime = 1f;
    long lastTime = System.nanoTime();

    long deathCubeLastSpawnTime = System.currentTimeMillis();

    List<Cube> cubes;
    public static List<DeathCube> deathCubes = new ArrayList<>();
    List<Triple> floor = new ArrayList<>();
    public static List<BulletHead> bullets = new ArrayList<>();
    List<PowerUp> powerUps = new ArrayList<>();

    public static final float GRAVITY = 10f;

    private DatagramSocket socket;

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

        new Thread(new Receiver(socket)).start();

        while(true) {
            long tickStart = System.currentTimeMillis();

            long now = System.currentTimeMillis();
            deltaTime = (now - lastTime) / 1000f;
            lastTime = now;

            for (Client c : clients) {
                boolean w, a, s, d, space, leftClick, rightClick;
                float rotationX, rotationY;

//                synchronized (c.inputLock) {
                    w = c.latestInput.w;
                    a = c.latestInput.a;
                    s = c.latestInput.s;
                    d = c.latestInput.d;
                    space = c.latestInput.space;
                    leftClick = c.latestInput.leftClick;
                    rightClick = c.latestInput.rightClick;
                    rotationX = c.latestInput.rotationX;
                    rotationY = c.latestInput.rotationY;

//                }
                useReceivedData(c, rotationX, rotationY, w, a, s, d, space, leftClick, rightClick);

                updatePerClient(c);
            }

            update();
            broadCastState();

            long tickDuration = System.currentTimeMillis() - tickStart;
            long sleep = Math.max(0, TICK_DURATION_MS - tickDuration);
            try {

                Thread.sleep((int)(sleep));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void update() {
        bullets.forEach(Shootable::update);
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
        handleBulletHittingSomething();

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

    private void useReceivedData(Client client, float rotationX, float rotationY, boolean w, boolean a, boolean s, boolean d, boolean space, boolean leftClick, boolean rightClick) {

        if(client.isDead)
            return;
        client.sum = new Triple(0f,0f,0f);

        client.cameraRotation.x = rotationX;
        client.cameraRotation.y = rotationY;

        if(w) client.sum.add(client.moveForward());
        if(a) client.sum.add(client.moveLeft());
        if(s) client.sum.add(client.moveBackward());
        if(d) client.sum.add(client.moveRight());
        if(space && !client.inAir) {
            client.speedY = 6f;
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


        if(rightClick && (System.currentTimeMillis() - client.lastSecondarySwitch) > 200) {
            client.lastSecondarySwitch = System.currentTimeMillis();
            client.grapplingEquipped = !client.grapplingEquipped;
            client.swinging = false;
            client.grapplingHead.shot = false;
            client.grapplingHead.flying = false;
        }
    }

    private void broadCastState() {
        for(Client client : clients) {
            senderBuffer.clear();

            senderBuffer.putFloat(client.cameraCoords.x);
            senderBuffer.putFloat(client.cameraCoords.y);
            senderBuffer.putFloat(client.cameraCoords.z);
            senderBuffer.putInt(client.health);

            senderBuffer.putInt(bullets.size());
            for (BulletHead bulletHead : bullets) {
                senderBuffer.putFloat(bulletHead.x);
                senderBuffer.putFloat(bulletHead.y);
                senderBuffer.putFloat(bulletHead.z);
                senderBuffer.putFloat(bulletHead.rotation.x);
                senderBuffer.putFloat(bulletHead.rotation.y);
                senderBuffer.put((byte) (bulletHead.shot ? 1 : 0));
                senderBuffer.put((byte) (bulletHead.flying ? 1 : 0));
            }
            senderBuffer.put((byte)(client.heldBullet != null ? 1 : 0));
            senderBuffer.put((byte)(client.grapplingEquipped? 1 : 0));
            senderBuffer.put((byte)(client.grapplingHead.shot? 1 : 0));
            senderBuffer.put((byte)(client.grapplingHead.flying? 1 : 0));

            senderBuffer.putFloat(client.grapplingHead.x);
            senderBuffer.putFloat(client.grapplingHead.y);
            senderBuffer.putFloat(client.grapplingHead.z);
            senderBuffer.putFloat(client.grapplingHead.rotation.x);
            senderBuffer.putFloat(client.grapplingHead.rotation.y);

            senderBuffer.putInt(clients.size()-1);
            for(Client c : clients) {
                if(c == client) continue;
                senderBuffer.putFloat(c.cameraCoords.x);
                senderBuffer.putFloat(c.cameraCoords.y);
                senderBuffer.putFloat(c.cameraCoords.z);
                senderBuffer.putFloat(c.cameraRotation.x);
                senderBuffer.putFloat(c.cameraRotation.y);

                senderBuffer.putFloat(c.grapplingHead.x);
                senderBuffer.putFloat(c.grapplingHead.y);
                senderBuffer.putFloat(c.grapplingHead.z);
                senderBuffer.putFloat(c.grapplingHead.rotation.x);
                senderBuffer.putFloat(c.grapplingHead.rotation.y);
                senderBuffer.put((byte)(c.grapplingEquipped? 1 : 0));
            }

            senderBuffer.putLong(client.time);
            senderBuffer.putLong(System.currentTimeMillis() + 4000);

            byte[] data = senderBuffer.array();
            client.packet.setData(data, 0,  senderBuffer.position());
            System.out.println(senderBuffer.position());
            try {
                socket.send(client.packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updatePerClient(Client client) {
        if(client.isDead && System.currentTimeMillis() - client.timeOfDeathLocal > 5000) {
            client.health = 100;
            client.isDead = false;
        }

        if(client.health <= 0 && !client.isDead) {
            client.cameraCoords.x = 0;
            client.cameraCoords.y = 20;
            client.cameraCoords.z = 0;
            client.isDead = true;
            client.timeOfDeathLocal = System.currentTimeMillis();
        }

        if(client.isDead) return;

        if(!client.swinging)
            client.speedY -= GRAVITY * deltaTime;
        float dy = client.speedY * deltaTime;
        client.sum.y += dy;
        client.cameraCoords.y += client.sum.y;

        for (Cube cube : cubes) {
            if (cube.isPointInCube(client.cameraCoords)) {
                if (client.sum.y > 0) {
                    client.cameraCoords.y = cube.y - cube.size / 2 - 0.0001f;
                } else {
                    client.cameraCoords.y = cube.y + cube.size / 2 + 0.0001f;
                    client.speedY = 0f;
                    client.inAir = false;
                }
                client.speedY = 0f;
            }
        }
        if (client.cameraCoords.y <= 0f) {
            client.cameraCoords.y = 0.0001f;
            client.speedY = 0f;
            client.inAir = false;
        }

        client.moveCharacter();

        for (Cube cube : cubes) {
            if (cube.isPointInCube(client.cameraCoords)) {
                if (client.speedZ > 0)
                    client.cameraCoords.z = cube.z - cube.size / 2 - 0.0001f;
                else if (client.speedZ < 0)
                    client.cameraCoords.z = cube.z + cube.size / 2 + 0.0001f;
                client.speedZ = 0f;
            }
        }

        if(client.swinging) {
            client.swingAround();
        }

        client.grapplingHead.update();

        if(client.heldBullet == null && System.currentTimeMillis() - client.bulletShotLastTime > 333){
            client.heldBullet = new BulletHead();
        }

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
    }

    public static void main(String[] args) {
        SimpleMove game = new SimpleMove();
        game.start();
    }

}
