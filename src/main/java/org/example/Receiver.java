package org.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

import static org.example.SimpleMove.clients;
import static org.example.SimpleMove.senderBuffer;

public class Receiver implements Runnable{

    private final DatagramSocket socket;

    public Receiver(DatagramSocket socket) {
        this.socket = socket;

    }

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
    }
}
