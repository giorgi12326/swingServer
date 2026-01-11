package org.example;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.*;

public class SimpleMove extends Canvas implements Runnable {
    Set<Client> clients = new HashSet<>();

    public static final float SCREEN_WIDTH = 1280f;
    public static final float SCREEN_HEIGHT = 720f;
    public static final float moveSpeed = 6f;
    public static  boolean deathCubeSpawnMode = false;
    public static boolean hit = false;
    public final Set<Integer> keysDown = new HashSet<>();
    public static float deltaTime = 1f;
    public static float FOV = 1;
    long lastTime = System.nanoTime();

    long bulletShotLastTime = System.currentTimeMillis();
    long deathCubeLastSpawnTime = System.currentTimeMillis(); // class-level variable

//    public static boolean swinging = false;
//    public static boolean client.grapplingEquipped = false;

//    public static Triple cameraCoords = new Triple(0f,0f,0f);
//    public static Pair<Float> cameraRotation = new Pair<>(0f,0f);

    List<Cube> cubes;
    List<DeathCube> deathCubes = new ArrayList<>();
    List<Triple> floor = new ArrayList<>();
    List<BulletHead> bullets = new ArrayList<>();


    public static final float GRAVITY = 10f;

//    private boolean inAir;
//    private Triple client.sum;

    Gun gun = new Gun(0,0,0);
//    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    private Triple anchor;
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

        setSize((int)SCREEN_WIDTH, (int)SCREEN_HEIGHT);

        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                floor.add(new Triple(i*1f, 0f, j*1f));
            }
        }
    }

    public void start() throws Exception {
        socket = new DatagramSocket(1234, InetAddress.getByName("0.0.0.0"));
        System.out.println("Socket Created");

        new Thread(this).start();

        new Thread(() -> {
            try {
                System.out.println("Server listening...");

                while (true) {
                    byte[] buf = new byte[32];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    boolean newClient = true;
                    Client client = null;
                    for(Client c : clients) {
                        if(packet.getAddress().toString().equals(c.ip.toString()) && packet.getPort() == c.port) {
                            client = c;
                            newClient = false;
                        }
                    }
                    if(newClient) {
                        client = new Client(packet.getAddress(), packet.getPort());
                        clients.add(client);
                        System.out.println("client connected with ip: " + packet.getAddress() + " and port: " + packet.getPort());
                    }

                    ByteBuffer buffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());

                    boolean w = buffer.get() == 1;
                    boolean a = buffer.get() == 1;
                    boolean s = buffer.get() == 1;
                    boolean d = buffer.get() == 1;
                    boolean space = buffer.get() == 1;
                    boolean leftClick = buffer.get() == 1;
                    boolean rightClick = buffer.get() == 1;
                    float rotationX = buffer.getFloat();
                    float rotationY = buffer.getFloat();

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
                        }
                    }
                    if(rightClick) {
                        client.grapplingEquipped = !client.grapplingEquipped;
                        client.swinging = false;
                        client.grapplingHead.shot = false;
                    }

                    for(Client c : clients) {
                        update(c);
                    }

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    public void run() {
        try {


            while (true) {

                for(Client client : clients) {
                    ByteBuffer buffer = ByteBuffer.allocate(12 + 4 + bullets.size() * 2 *(4 * 5 + 2) + 2 + (4 + (clients.size()-1)*(5*4 + 1))); // 4 bytes per float * 3
                    buffer.putFloat(client.cameraCoords.x);
                    buffer.putFloat(client.cameraCoords.y);
                    buffer.putFloat(client.cameraCoords.z);

                    buffer.putInt(bullets.size());
                    for (BulletHead bulletHead : bullets) {
                        buffer.putFloat(bulletHead.x);
                        buffer.putFloat(bulletHead.y);
                        buffer.putFloat(bulletHead.z);
                        buffer.putFloat(bulletHead.rotation.x);
                        buffer.putFloat(bulletHead.rotation.y);
                        buffer.put((byte) (bulletHead.shot ? 1 : 0));
                        buffer.put((byte) (bulletHead.flying ? 1 : 0));
                    }
                    buffer.put((byte)(client.heldBullet != null ? 1 : 0));
                    buffer.put((byte)(client.grapplingEquipped? 1 : 0));

                    buffer.putInt(clients.size()-1);
                    for(Client c : clients) {
                        if(c == client)
                            continue;
                        buffer.putFloat(c.cameraCoords.x);
                        buffer.putFloat(c.cameraCoords.y);
                        buffer.putFloat(c.cameraCoords.z);
                        buffer.putFloat(c.cameraRotation.x);
                        buffer.putFloat(c.cameraRotation.y);
                        buffer.put((byte)(c.grapplingEquipped? 1 : 0));
                    }


                    byte[] data = buffer.array();

                    socket.send(new DatagramPacket(data, data.length, client.ip, client.port));
                }

                long now = System.nanoTime();
                deltaTime = (now - lastTime) / 1_000_000_000f;
                lastTime = now;

                Thread.sleep(5);   // ~60 FPS
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void update(Client client) {
        for(Cube cube : cubes) {
            cube.update();
        }
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
            swingAround(anchor,client);
        }

        client.grapplingHead.update();

        if(!client.grapplingHead.shot){
            client.grapplingHead.x = client.cameraCoords.x + 0.1f;
            client.grapplingHead.y = client.cameraCoords.y;
            client.grapplingHead.z = client.cameraCoords.z + 1f;
        }

        for(BulletHead bulletHead: bullets) {
            bulletHead.update();
        }

        if(client.heldBullet != null){
            client.heldBullet.x = client.cameraCoords.x;
            client.heldBullet.y = client.cameraCoords.y- 0.15f;
            client.heldBullet.z = client.cameraCoords.z + 0.8f;

        }
        else if(System.currentTimeMillis() - bulletShotLastTime > 200){
            client.heldBullet = new BulletHead();
            client.heldBullet.x = 1000f;
        }

        gun.x = client.cameraCoords.x + 0.1f;
        gun.y = client.cameraCoords.y;
        gun.z = client.cameraCoords.z + 0.3f;

        if(client.grapplingHead.shot)
            for(Cube cube : cubes) {
                if(cube.isPointInCube(client.grapplingHead.getNodes()[16])) {
                    client.swinging = true;
                    client.grapplingHead.flying = false;
                    anchor = new Triple(cube.x + cube.size / 2f, cube.y + cube.size / 2f, cube.z + cube.size / 2f);
                }
            }

        for (BulletHead bullet : bullets) {
            for (int j = deathCubes.size()-1; j >= 0; j--) {
                DeathCube deathCube = deathCubes.get(j);
                if (deathCube.isPointInCube(bullet.getNodes()[8]))
                    deathCubes.remove(j);
            }
        }
        boolean localHit = false;
        for(DeathCube deathCube : deathCubes) {
            if(deathCube.isPointInCube(client.cameraCoords))
                localHit = true;
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

        for (int i = bullets.size() - 1; i >= 0; i--) {
            BulletHead bullet = bullets.get(i);
            if(bullet.markAsDeleted)
                bullets.remove(i);
        }

        hit = localHit;

    }

    private void moveCharacter(Client client) {
        float inputX = client.sum.x;
        float inputZ = client.sum.z;

        float inputLength = (float)Math.sqrt(inputX*inputX + inputZ*inputZ);
        if(inputLength > 0.001f) {
            inputX /= inputLength;
            inputZ /= inputLength;
        }

        inputX *= moveSpeed;
        inputZ *= moveSpeed;

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

    private void clearScreen(Graphics2D g) {
        if(hit)
            g.setColor(Color.RED);
        else
            g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawCrosshair(Graphics2D g) {
        g.setColor(Color.lightGray);
        g.fill(new Rectangle2D.Float(SCREEN_WIDTH/2f - 10f, SCREEN_HEIGHT/2f-2f, 20f, 4f));
        g.fill(new Rectangle2D.Float(SCREEN_WIDTH/2f - 2f, SCREEN_HEIGHT/2f-10f, 4f, 20f));
    }

    private Triple rotationToDirection(Pair<Float> rotation) {
        float dx = (float)(Math.cos(rotation.x) * Math.sin(rotation.y));
        float dy = (float)(Math.sin(rotation.x));
        float dz = (float)(Math.cos(rotation.x) * Math.cos(rotation.y));
        return new Triple(dx, dy, dz);
    }


    public static void main(String[] args) {
        SimpleMove canvas = new SimpleMove();


        try {
            canvas.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Triple moveForward(Client client) {
        float dx = moveSpeed * (float) Math.sin(client.cameraRotation.y) * deltaTime;
        float dz = moveSpeed * (float) Math.cos(client.cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    private Triple moveBackward(Client client) {
        float dx = -moveSpeed * (float) Math.sin(client.cameraRotation.y) * deltaTime;
        float dz = -moveSpeed * (float) Math.cos(client.cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    private Triple moveRight(Client client) {
        float dx = moveSpeed * (float) Math.cos(client.cameraRotation.y) * deltaTime;
        float dz = moveSpeed * (float) -Math.sin(client.cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    private Triple moveLeft(Client client) {
        float dx = -moveSpeed * (float)Math.cos(client.cameraRotation.y) * deltaTime;
        float dz = moveSpeed * (float)Math.sin(client.cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public void swingAround(Triple anchor, Client client) {
        Triple toAnchor = anchor.sub(client.cameraCoords).normalize();
        Triple tangent = toAnchor.normalize();
        client.cameraCoords = client.cameraCoords.add(tangent.scale(moveSpeed*2 * deltaTime));
    }


    private void prepareBulletForFlying(Pair<Float> direction, Client client) {
        if(client.heldBullet == null)
            return;

        prepareShootableForFlying(direction, client.heldBullet, client);

        bullets.add(client.heldBullet);
        client.heldBullet = null;
        bulletShotLastTime = System.currentTimeMillis();

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
