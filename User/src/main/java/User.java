import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.distribution.ZipfDistribution;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class User {

    static DatagramSocket socket;
    static Address router;
    static int size;
    static int totalRequest;
    static ZipfDistribution generator;

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

    public static void main(String[] args) {

        try{

            if(args.length < 4) throw new Exception();

            int port = Integer.parseInt(args[0]);
            socket = new DatagramSocket(port);
            size = Integer.parseInt(args[1]);
            String routerAddress = args[2];
            Double exponent = Double.valueOf(args[3]);

            router = new Address();
            router.ipAddress = InetAddress.getByName(routerAddress.split(":")[0]);
            router.port = Integer.parseInt(routerAddress.split(":")[1]);
            totalRequest = 0;
            generator = new ZipfDistribution(size, exponent);
            for(int i = 0; i < 100 ; i++){
                Thread.sleep(1000);
                makeRequest(generator.sample()+".txt");
            }
            System.out.println(totalRequest);
            Packet req = new Packet();
            req.setType("print");
            byte[] buffer = new byte[65527];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            request.setAddress(router.ipAddress);
            request.setPort(router.port);
            request.setData(req.serialize());
            socket.send(request);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            socket.close();
        }

    }

}
