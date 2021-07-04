import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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
            System.out.println("Received request for : "+dataName);
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

    private void sendTotal(Address source) throws IOException {
        byte[] buffer = new byte[65527];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        packet.setAddress(source.ipAddress);
        packet.setPort(source.port);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(totalReq);
        dataOut.close();
        byte[] data = byteOut.toByteArray();
        packet.setData(data);
        socket.send(packet);
        totalReq = 0;
    }

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
                if(incomingPacket.getType().equals("interest")) {
                    new HandleRequest(source, incomingPacket.getName()).start();
                }
                else if(incomingPacket.getType().equals("getHits")){
                    System.out.println("Get Hits");
                    sendTotal(source);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            System.out.println("Primary Server started on port : "+port);
            primaryServer.acceptRequests();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
