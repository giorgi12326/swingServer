package org.example;

import org.example.powerUp.PowerUp;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class SimpleMove {
    final long TICK_RATE = 60;
    final long TICK_DURATION_MS = 1000 / TICK_RATE; // ≈16 ms per tick

    Set<Client> clients = new HashSet<>();
    ByteBuffer senderBuffer = ByteBuffer.allocate(4096); // 4 bytes per float * 3

    public static  boolean deathCubeSpawnMode = false;
    public static float deltaTime = 1f;
    long lastTime = System.nanoTime();

    long deathCubeLastSpawnTime = System.currentTimeMillis();

    List<Cube> cubes;
    List<DeathCube> deathCubes = new ArrayList<>();
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

        new Thread(() -> {
            try {
                System.out.println("Server listening...");

                byte[] buf = new byte[32];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                ByteBuffer readerBuffer = ByteBuffer.wrap(buf);
                while (true) {
                    socket.receive(packet);

                    readerBuffer.position(0);
                    readerBuffer.limit(packet.getLength());

                    boolean newClient = true;
                    Client client = null;
                    for(Client c : clients) {
                        if(packet.getAddress().toString().equals(c.ip.toString()) && packet.getPort() == c.port) {
                            client = c;
                            newClient = false;
                        }
                    }
                    if(newClient) {
                        client = new Client(packet.getAddress(), packet.getPort(),senderBuffer);
                        clients.add(client);
                        System.out.println("client connected with ip: " + packet.getAddress() + " and port: " + packet.getPort());
                    }

//                    synchronized(client.inputLock) {
                        client.latestInput.w = readerBuffer.get() == 1;
                        client.latestInput.a = readerBuffer.get() == 1;
                        client.latestInput.s = readerBuffer.get() == 1;
                        client.latestInput.d = readerBuffer.get() == 1;
                        client.latestInput.space = readerBuffer.get() == 1;
                        client.latestInput.leftClick = readerBuffer.get() == 1;
                        client.latestInput.rightClick = readerBuffer.get() == 1;
                        client.latestInput.rotationX = readerBuffer.getFloat();
                        client.latestInput.rotationY = readerBuffer.getFloat();
                        client.time = readerBuffer.getLong();
//                    }

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

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
        for(BulletHead bulletHead: bullets) {
            bulletHead.update();
        }

        for (int i = deathCubes.size() - 1; i >= 0; i--) {
            DeathCube deathCube = deathCubes.get(i);
            if(deathCube.markedAsDeleted || deathCube.y < 0)
                deathCubes.remove(i);
        }

        for (BulletHead bullet : bullets) {
            for (int j = deathCubes.size()-1; j >= 0; j--) {
                DeathCube deathCube = deathCubes.get(j);
                if (deathCube.isPointInCube(bullet.getNodes()[8]))
                    deathCubes.remove(j);
            }
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            BulletHead bullet = bullets.get(i);
            if(bullet.markAsDeleted)
                bullets.remove(i);
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
                    client.health -= 10;
                }
            }
        }

    }

    private void useReceivedData(Client client, float rotationX, float rotationY, boolean w, boolean a, boolean s, boolean d, boolean space, boolean leftClick, boolean rightClick) {

        client.sum = new Triple(0f,0f,0f);

        client.cameraRotation.x = rotationX;
        client.cameraRotation.y = rotationY;

        if(w) client.sum.add(moveForward(client));
        if(a) client.sum.add(moveLeft(client));
        if(s) client.sum.add(moveBackward(client));
        if(d) client.sum.add(moveRight(client));
        if(space && !client.inAir) {
            client.speedY = 6f;
            client.inAir = true;
        }

        if(leftClick) {
            Ray ray = new Ray(new Triple(client.cameraCoords), new Pair<>(client.cameraRotation), 5f);
            if(client.grapplingEquipped && !client.grapplingHead.shot)
                prepareShootableForFlying(ray.direction, client.grapplingHead, client);
            else
                prepareBulletForFlying(ray.direction, client);
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

        moveCharacter(client);

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
            swingAround(client);
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

        if (deathCubeSpawnMode && System.currentTimeMillis() - deathCubeLastSpawnTime > 1000) {
            deathCubeLastSpawnTime = System.currentTimeMillis();
            spawnCubeRandomlyAtDistance(64f, client);
        }
        client.hitbox.x = client.cameraCoords.x;
        client.hitbox.y = client.cameraCoords.y;
        client.hitbox.z = client.cameraCoords.z;
    }

    private void moveCharacter(Client client) {
        float inputX = client.sum.x;
        float inputZ = client.sum.z;

        float inputLength = (float)Math.sqrt(inputX*inputX + inputZ*inputZ);
        if(inputLength > 0.001f) {
            inputX /= inputLength;
            inputZ /= inputLength;
        }

        inputX *= client.moveSpeed;
        inputZ *= client.moveSpeed;

        final float DRAG_MOVE = 0.1f;
        final float DRAG_IDLE = 30.0f;
        boolean notMoving = client.sum.x == 0 && client.sum.z == 0;

        float drag = DRAG_MOVE;

        float dot = client.speedX * inputX + client.speedZ * inputZ;

        if ((notMoving && !client.inAir) || (dot < 0f && !client.inAir)) {
            drag = DRAG_IDLE;
        }

        client.speedX -= client.speedX * drag * deltaTime;
        client.speedZ -= client.speedZ * drag * deltaTime;

        client.speedX += inputX * deltaTime;
        client.speedZ += inputZ * deltaTime;

        float maxSpeed = 5f;
        float combinedSpeed = (float)Math.sqrt(client.speedX*client.speedX + client.speedZ*client.speedZ);
        if(combinedSpeed > maxSpeed) {
            client.speedX = client.speedX / combinedSpeed * maxSpeed;
            client.speedZ = client.speedZ / combinedSpeed * maxSpeed;
        }

        client.cameraCoords.x += client.speedX * deltaTime;
        client.cameraCoords.z += client.speedZ * deltaTime;
    }


    private void spawnCubeRandomlyAtDistance(float radius, Client client) {
        float x = (float)(Math.random() * 2 * radius - radius);
        float z = (float)(Math.random() * 2 * radius - radius);
        float y = 10f;
        deathCubes.add(new DeathCube(client.cameraCoords.x + x, client.cameraCoords.y + y, client.cameraCoords.z + z, client.cameraCoords, 1f));

    }

    private Triple rotationToDirection(Pair<Float> rotation) {
        float dx = (float)(Math.cos(rotation.x) * Math.sin(rotation.y));
        float dy = (float)(Math.sin(rotation.x));
        float dz = (float)(Math.cos(rotation.x) * Math.cos(rotation.y));
        return new Triple(dx, dy, dz);
    }

    public static void main(String[] args) {
        SimpleMove game = new SimpleMove();

        game.start();

    }

    private Triple moveForward(Client client) {
        float dx = client.moveSpeed * (float) Math.sin(client.cameraRotation.y) * deltaTime;
        float dz = client.moveSpeed * (float) Math.cos(client.cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    private Triple moveBackward(Client client) {
        float dx = -client.moveSpeed * (float) Math.sin(client.cameraRotation.y) * deltaTime;
        float dz = -client.moveSpeed * (float) Math.cos(client.cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    private Triple moveRight(Client client) {
        float dx = client.moveSpeed * (float) Math.cos(client.cameraRotation.y) * deltaTime;
        float dz = client.moveSpeed * (float) -Math.sin(client.cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    private Triple moveLeft(Client client) {
        float dx = -client.moveSpeed * (float)Math.cos(client.cameraRotation.y) * deltaTime;
        float dz = client.moveSpeed * (float)Math.sin(client.cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public void swingAround(Client client) {
        Triple toAnchor = client.anchor.sub(client.cameraCoords).normalize();
        Triple tangent = toAnchor.normalize();
        client.cameraCoords = client.cameraCoords.add(tangent.scale(client.moveSpeed*2 * deltaTime));
        if(client.anchor.sub(client.cameraCoords).length() <= 1f) {
            client.inAir = true;
            client.speedY += 10f;
            client.swinging = false;
            client.grapplingEquipped = false;
            client.grapplingHead.shot = false;
            client.grapplingHead.flying = false;
        }
    }

    private void prepareBulletForFlying(Pair<Float> direction, Client client) {
        if(client.heldBullet == null)
            return;
        BulletHead e = new BulletHead();
        e.rotation = new Pair<>(e.rotation.x,e.rotation.y);

        client.heldBullet = null;

        prepareShootableForFlying(direction, e, client);
        bullets.add(e);
        client.bulletShotLastTime = System.currentTimeMillis();
    }

    private void prepareShootableForFlying(Pair<Float> direction, Shootable shootable, Client client) {
        shootable.direction = rotationToDirection(direction);
        shootable.rotation = new Pair<>(client.cameraRotation.x, client.cameraRotation.y);
        Triple newPosition = new Triple(client.cameraCoords.x, client.cameraCoords.y - 0.15f, client.cameraCoords.z + 0.8f).rotateXY(client.cameraCoords, shootable.rotation);
        shootable.x = newPosition.x;
        shootable.y = newPosition.y;
        shootable.z = newPosition.z;
        shootable.shot = true;
        shootable.flying = true;
        client.heldBullet = null; //fix if more bullets
        client.bulletShotLastTime = System.currentTimeMillis();
    }
}
