import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.*;

public class TestUser {

    static DatagramSocket socket;
    static Address router;
    static int totalRequest;


    static void makeRequest(String dataName) throws IOException {
        Packet req = new Packet();
        req.setType("interest");
        req.setName(dataName);
        byte[] buffer = new byte[65527];
        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
        request.setAddress(router.ipAddress);
        request.setPort(router.port);
        request.setData(req.serialize());
        socket.send(request);
        totalRequest ++;
        buffer = new byte[65527];
        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
        socket.receive(reply);
        Packet replyPacket = SerializationUtils.deserialize(reply.getData());
        System.out.println("Received data for name : "+replyPacket.getName());
    }

    public static void main(String[] args) throws IOException {

        socket = new DatagramSocket(311);
        router = new Address();
        router.ipAddress = InetAddress.getByName("192.168.50.52");
        router.port = 305;
        makeRequest("1.txt");

    }

}
