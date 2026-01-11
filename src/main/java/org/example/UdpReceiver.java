package org.example;

import java.net.*;

public class UdpReceiver {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(1234, InetAddress.getByName("0.0.0.0"));

        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        System.out.println("Waiting...");
        socket.receive(packet); // blocks

        String msg = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Received: " + msg);

        socket.close();
    }
}