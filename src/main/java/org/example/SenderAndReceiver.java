package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import static org.example.SimpleMove.*;

public class SenderAndReceiver implements Runnable{

    @Override
    public void run() {
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

              synchronized(client.mutex) {
                client.latestInput.w = readerBuffer.get() == 1;
                client.latestInput.a = readerBuffer.get() == 1;
                client.latestInput.s = readerBuffer.get() == 1;
                client.latestInput.d = readerBuffer.get() == 1;
                client.latestInput.space = readerBuffer.get() == 1;
                client.latestInput.one = readerBuffer.get() == 1;
                client.latestInput.two = readerBuffer.get() == 1;
                client.latestInput.three = readerBuffer.get() == 1;
                client.latestInput.leftClick = readerBuffer.get() == 1;
                client.latestInput.rightClick = readerBuffer.get() == 1;
                client.latestInput.rotationX = readerBuffer.getFloat();
                client.latestInput.rotationY = readerBuffer.getFloat();
                client.time = readerBuffer.getLong();
              }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void broadCastState() {
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
            try {

                socket.send(client.packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
