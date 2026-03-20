package org.example;

import org.example.powerUp.MoveSpeedPowerUp;
import org.example.powerUp.PowerUp;
import org.example.powerUp.ShootSpeedPowerUp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.example.SimpleMove.*;

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
    final float moveSpeed = 100f;
    ClientInput latestInput = new ClientInput();
    final Object mutex = new Object();
    DatagramPacket packet;
    long time;

    Cube hitbox = new Cube(0,0,0,1f);

    float speedX = 0f;
    float speedY = 0f;
    float speedZ = 0f;
    long bulletShotLastTime = System.currentTimeMillis();
    List<PowerUp> powerUps = new ArrayList<>();

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
        float dx = getMoveSpeed() * (float) Math.sin(cameraRotation.y) * deltaTime;
        float dz = getMoveSpeed() * (float) Math.cos(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public Triple moveBackward() {
        float dx = -getMoveSpeed() * (float) Math.sin(cameraRotation.y) * deltaTime;
        float dz = -getMoveSpeed() * (float) Math.cos(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public Triple moveRight() {
        float dx = getMoveSpeed() * (float) Math.cos(cameraRotation.y) * deltaTime;
        float dz = getMoveSpeed() * (float) -Math.sin(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public Triple moveLeft() {
        float dx = -getMoveSpeed() * (float)Math.cos(cameraRotation.y) * deltaTime;
        float dz = getMoveSpeed() * (float)Math.sin(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    public void moveCharacter() {
        sum.x /= 1.3f;
        sum.z /= 1.3f;

        sum.x += speedX;
        sum.z += speedZ;

        float maxSpeed = 3f * getMoveSpeed()/moveSpeed;
        float combinedSpeed = (float)Math.sqrt(sum.x*sum.x + sum.z*sum.z);
        if(combinedSpeed > maxSpeed) {
            sum.x = sum.x / combinedSpeed * maxSpeed;
            sum.z = sum.z / combinedSpeed * maxSpeed;
        }

        if(!swinging && inAir)
            speedY = -GRAVITY * deltaTime;
        sum.y += speedY * deltaTime;
        cameraCoords.y += sum.y;
        clipBackForYAndGround();

        cameraCoords.x += sum.x * deltaTime;
        clipBackForX();

        cameraCoords.z += sum.z * deltaTime;
        clipBackForZ();
    }

    private void clipBackForZ() {
        for (Cube cube : cubes) {
            if (cube.isPointInCube(cameraCoords)) {
                if (sum.z > 0)
                    cameraCoords.z = cube.z - cube.size / 2 - 0.0001f;
                else if (sum.z < 0)
                    cameraCoords.z = cube.z + cube.size / 2 + 0.0001f;
                sum.z = 0f;
            }
        }
    }

    private void clipBackForX() {
        for (Cube cube : cubes) {
            if (cube.isPointInCube(cameraCoords)) {
                if (sum.x > 0)
                    cameraCoords.x = cube.x - cube.size / 2 - 0.0001f;
                else if (sum.x < 0)
                    cameraCoords.x = cube.x + cube.size / 2 + 0.0001f;
                sum.x = 0f;
            }
        }
    }
    private void clipBackForYAndGround() {
        inAir = true;
        for (Cube cube : cubes) {
            if (cube.isPointInCube(cameraCoords)) {
                if (sum.y > 0) {
                    cameraCoords.y = cube.y - cube.size / 2 - 0.0001f;
                    sum.y = 0;
                } else {
                    cameraCoords.y = cube.y + cube.size / 2 + 0.0001f;
                    inAir = false;
                }
                speedY = 0f;
            }
        }

        if (cameraCoords.y <= 0f) {
            cameraCoords.y = 0.0001f;
            speedY = 0f;
            inAir = false;
        }
    }

    public void handleIfDead() {
        if(isDead && System.currentTimeMillis() - timeOfDeathLocal > 5000) {
            health = 100;
            isDead = false;
        }

        if(health <= 0 && !isDead) {
            cameraCoords.x = 0;
            cameraCoords.y = 20;
            cameraCoords.z = 0;
            isDead = true;
            timeOfDeathLocal = System.currentTimeMillis();
        }
    }

    public void swingAround() {
        Triple toAnchor = anchor.sub(cameraCoords).normalize();
        Triple tangent = toAnchor.normalize();
        cameraCoords = cameraCoords.add(tangent.scale(10 * deltaTime));
        if(anchor.sub(cameraCoords).length() <= 1f) {
            inAir = true;
            sum.y = 10f * deltaTime;
            swinging = false;
            grapplingEquipped = false;
            grapplingHead.shot = false;
            grapplingHead.flying = false;
        }
    }

    void update(){
        updatePowerUps();

    }

    private void updatePowerUps() {
        for (int i = powerUps.size()-1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            if(powerUp.isExpired()){
                powerUps.remove(powerUp);
            }
        }
    }

    private float getMoveSpeed() {
        float appliedMoveSpeed = moveSpeed;
        for (PowerUp powerUp: powerUps){
            if(powerUp instanceof MoveSpeedPowerUp){
                appliedMoveSpeed *= powerUp.effectiveness;
            }
        }

        return appliedMoveSpeed;
    }

    public float getAttackSpeedDuration() {
        long attackSpeedDuration = 333L;
        long appliedAttackSpeedDuration = attackSpeedDuration;
        for (PowerUp powerUp: powerUps){
            if(powerUp instanceof ShootSpeedPowerUp){
                appliedAttackSpeedDuration /= (long) powerUp.effectiveness;
            }
        }
        return appliedAttackSpeedDuration;
    }

}