import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server {

    class HandleRequest extends Thread{

        Address source;
        String dataName;

        HandleRequest(Address source, String dataName){
            this.source = source;
            this.dataName = dataName;
        }

        public void run(){
            totalReq ++;
            System.out.println("Received request for : "+dataName+" || Total Req : "+totalReq);
            String path = "DataFiles/Files/"+dataName;
            File file = new File(path);
            try {
                byte[] data = Files.readAllBytes(Paths.get(path));
                Packet packet = new Packet();
                packet.setType("data");
                packet.setName(dataName);
                packet.setData(data);
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
                datagramPacket.setAddress(source.ipAddress);
                datagramPacket.setPort(source.port);
                datagramPacket.setData(packet.serialize());
                try {
                    socket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    static DatagramSocket socket;
    static int totalReq;

    private void acceptRequests(){
        try {
            while (true) {
                byte[] buffer = new byte[65527];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                Packet incomingPacket = SerializationUtils.deserialize(request.getData());
                Address source = new Address();
                source.ipAddress = request.getAddress();
                source.port = request.getPort();
                if(incomingPacket.getType().equals("interest")) new HandleRequest(source, incomingPacket.getName()).start();
            }

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (socket != null) socket.close();
        }
    }

    public static void main(String[] args) {
        try {

            if(args.length < 1) throw new Exception();
            int port = Integer.parseInt(args[0]);
            socket = new DatagramSocket(port);
            totalReq = 0;
            Server primaryServer = new Server();
            primaryServer.acceptRequests();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
