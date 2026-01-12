package org.example;

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
            socket = new DatagramSocket(1247, InetAddress.getByName("0.0.0.0"));
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

                    boolean w = readerBuffer.get() == 1;
                    boolean a = readerBuffer.get() == 1;
                    boolean s = readerBuffer.get() == 1;
                    boolean d = readerBuffer.get() == 1;
                    boolean space = readerBuffer.get() == 1;
                    boolean leftClick = readerBuffer.get() == 1;
                    boolean rightClick = readerBuffer.get() == 1;
                    float rotationX = readerBuffer.getFloat();
                    float rotationY = readerBuffer.getFloat();

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

                    synchronized(client.inputLock) {
                        client.latestInput.w = w;
                        client.latestInput.a = a;
                        client.latestInput.s = s;
                        client.latestInput.d = d;
                        client.latestInput.space = space;
                        client.latestInput.leftClick = leftClick;
                        client.latestInput.rightClick = rightClick;
                        client.latestInput.rotationX = rotationX;
                        client.latestInput.rotationY = rotationY;
                    }

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

                synchronized (c.inputLock) {
                    w = c.latestInput.w;
                    a = c.latestInput.a;
                    s = c.latestInput.s;
                    d = c.latestInput.d;
                    space = c.latestInput.space;
                    leftClick = c.latestInput.leftClick;
                    rightClick = c.latestInput.rightClick;
                    rotationX = c.latestInput.rotationX;
                    rotationY = c.latestInput.rotationY;

                }
                useReceivedData(c,
                        rotationX, rotationY,
                        w, a, s, d,
                        space,
                        leftClick, rightClick);

                update(c);
            }
            try {
                broadCastState();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            long tickDuration =  System.currentTimeMillis() - tickStart;
            try {
                long sleepTime = Math.max(0, TICK_DURATION_MS - tickDuration);

                Thread.sleep((int)(Math.random()*32));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void useReceivedData(Client client, float rotationX, float rotationY, boolean w, boolean a, boolean s, boolean d, boolean space, boolean leftClick, boolean rightClick) {

        client.sum = new Triple(0f,0f,0f);

        client.cameraRotation.x = rotationX;
        client.cameraRotation.y = rotationY;

        if(w) client.cameraCoords = client.cameraCoords.add(moveForward(client));
        if(a) client.cameraCoords = client.cameraCoords.add(moveLeft(client));
        if(s) client.cameraCoords = client.cameraCoords.add(moveBackward(client));
        if(d) client.cameraCoords = client.cameraCoords.add(moveRight(client));
        if(space && !client.inAir) {
            client.speedY = 6f;
            client.inAir = true;
        }

        if(leftClick) {
            Ray ray = new Ray(new Triple(client.cameraCoords), new Pair<>(client.cameraRotation), 5f);
            if(client.grapplingEquipped && !client.grapplingHead.shot){
                prepareShootableForFlying(ray.direction, client.grapplingHead, client);
            }
            else {
                prepareBulletForFlying(ray.direction, client);
                System.out.println(bullets.size());
            }
        }
        if(rightClick) {
            client.grapplingEquipped = !client.grapplingEquipped;
            client.swinging = false;
            client.grapplingHead.shot = false;
        }
    }


    private void broadCastState() throws IOException {
        for(Client client : clients) {
            senderBuffer.clear();
            senderBuffer.putFloat(client.cameraCoords.x);
            senderBuffer.putFloat(client.cameraCoords.y);
            senderBuffer.putFloat(client.cameraCoords.z);

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

            senderBuffer.putInt(clients.size()-1);
            for(Client c : clients) {
                if(c == client) continue;
                senderBuffer.putFloat(c.cameraCoords.x);
                senderBuffer.putFloat(c.cameraCoords.y);
                senderBuffer.putFloat(c.cameraCoords.z);
                senderBuffer.putFloat(c.cameraRotation.x);
                senderBuffer.putFloat(c.cameraRotation.y);
                senderBuffer.put((byte)(c.grapplingEquipped? 1 : 0));
            }

            senderBuffer.putLong(System.currentTimeMillis());

            byte[] data = senderBuffer.array();
            client.packet.setData(data, 0,  data.length);

            socket.send(client.packet);
        }
    }


    private void update(Client client) {
        for(DeathCube deathCube : deathCubes) {
            deathCube.update();
        }
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

        for(BulletHead bulletHead: bullets) {
            bulletHead.update();
        }

        if(client.heldBullet == null && System.currentTimeMillis() - client.bulletShotLastTime > 200){
            client.heldBullet = new BulletHead();
//            client.heldBullet.x = 1000f;
        }

        if(client.grapplingHead.shot)
            for(Cube cube : cubes) {
                if(cube.isPointInCube(client.grapplingHead.getNodes()[16])) {
                    client.swinging = true;
                    client.grapplingHead.flying = false;
                    client.anchor = new Triple(cube.x + cube.size / 2f, cube.y + cube.size / 2f, cube.z + cube.size / 2f);
                }
            }

        for (BulletHead bullet : bullets) {
            for (int j = deathCubes.size()-1; j >= 0; j--) {
                DeathCube deathCube = deathCubes.get(j);
                if (deathCube.isPointInCube(bullet.getNodes()[8]))
                    deathCubes.remove(j);
            }
        }
        for(DeathCube deathCube : deathCubes) {
            if(deathCube.isPointInCube(client.cameraCoords)){}
                //client Hit
        }

        if (deathCubeSpawnMode && System.currentTimeMillis() - deathCubeLastSpawnTime > 1000) {
            deathCubeLastSpawnTime = System.currentTimeMillis();
            spawnCubeRandomlyAtDistance(64f, client);
        }

        for (int i = deathCubes.size() - 1; i >= 0; i--) {
            DeathCube deathCube = deathCubes.get(i);
            if(deathCube.markedAsDeleted || deathCube.y < 0)
                deathCubes.remove(i);
        }

//        for (int i = bullets.size() - 1; i >= 0; i--) {
//            BulletHead bullet = bullets.get(i);
//            if(bullet.markAsDeleted)
//                bullets.remove(i);
//        }

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
        final float DRAG_IDLE = 12.0f;
        boolean notMoving = client.sum.x == 0 && client.sum.z == 0;

        float drag = DRAG_MOVE;

        float dot = client.speedX * inputX + client.speedZ * inputZ;

        if (notMoving || dot < 0f) {
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
    }


    private void prepareBulletForFlying(Pair<Float> direction, Client client) {
        if(client.heldBullet == null)
            return;
        BulletHead e = new BulletHead();
        e.x = client.heldBullet.x;
        e.y = client.heldBullet.y;
        e.z = client.heldBullet.z;
        e.rotation = new Pair<>(e.rotation.x,e.rotation.y);

        client.heldBullet = null;

        prepareShootableForFlying(direction, e, client);
        bullets.add(e);
        client.bulletShotLastTime = System.currentTimeMillis();
    }

    private void prepareShootableForFlying(Pair<Float> direction, Shootable shootable, Client client) {
        shootable.direction = rotationToDirection(direction);
        shootable.rotation = new Pair<>(client.cameraRotation.x, client.cameraRotation.y);
        Triple newPosition = new Triple(shootable.x, shootable.y, shootable.z).rotateXY(client.cameraCoords, shootable.rotation);
        shootable.x = newPosition.x;
        shootable.y = newPosition.y;
        shootable.z = newPosition.z;
        shootable.shot = true;
        shootable.flying = true;
    }
}
