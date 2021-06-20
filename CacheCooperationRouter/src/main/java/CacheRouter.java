import org.apache.commons.lang3.SerializationUtils;
import org.apache.hadoop.util.bloom.CountingBloomFilter;
import org.apache.hadoop.util.bloom.Key;
import org.apache.hadoop.util.hash.Hash;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;


public class CacheRouter {

    class HandleInterest extends Thread{

        Packet interest;
        Address source;

        public HandleInterest(Packet interest, Address source) {
            this.interest = interest;
            this.source = source;
        }

        private void forwardRequest(Address nextRouter) throws IOException {
            byte[] buffer = new byte[65527];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            datagramPacket.setAddress(nextRouter.ipAddress);
            datagramPacket.setPort(nextRouter.port);
            datagramPacket.setData(interest.serialize());
            socket.send(datagramPacket);
        }

        public void run() {

            String key = interest.getName();
            HashSet<Address> temp = PIT.get(key);
            if(temp == null){
                temp = new HashSet<>();
            }
            temp.add(source);
            PIT.put(key, temp);

            Address nextHop = primaryServer;
            if(networkCacheSummary.membershipTest(new Key(key.getBytes()))){
                Address router = cacheControlServer.get(key);
                if(router != null){
                    nextHop = router;
                }
            }
            try {
                forwardRequest(nextHop);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class HandleData extends Thread{

        Packet data;
        Address source;

        HandleData(Packet data, Address source){
            this.data = data;
            this.source = source;
        }

        private void sendData(Packet data, Address destination) throws IOException {
            byte[] buffer = new byte[65527];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            datagramPacket.setAddress(destination.ipAddress);
            datagramPacket.setPort(destination.port);
            datagramPacket.setData(data.serialize());
            socket.send(datagramPacket);
        }

        public void run(){

            synchronized (PIT){
                HashSet<Address> entryList = PIT.get(data.getName());
                if(entryList.size() > 0){
                    for(Address entry : entryList){
                        try {
                            sendData(data, entry);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    PIT.remove(data.getName());
                }
            }
        }


    }

    static ControlServer cacheControlServer;
    static HashMap<String, HashSet<Address>> PIT;
    static Address primaryServer;
    static DatagramSocket socket;
    static CountingBloomFilter networkCacheSummary;
    static int totalReq;

    private void acceptRequests(){

        try {
            while (true) {
                byte[] buffer = new byte[65527];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                Packet incomingPacket = SerializationUtils.deserialize(request.getData());
                System.out.println("Received request type : "+incomingPacket.getType()+ " | name : "+incomingPacket.getName());
                Address source = new Address();
                source.ipAddress = request.getAddress();
                source.port = request.getPort();
                switch (incomingPacket.getType()){
                    case "interest":
                        totalReq ++;
                        System.out.println("Total Requests : "+totalReq);
                        new HandleInterest(incomingPacket, source).start();
                        break;
                    case "data":
                        new HandleData(incomingPacket, source).start();
                        break;
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

            if(args.length < 2) throw new Exception();

            int nodePort = Integer.parseInt(args[0]);
            String primaryServerAddress = args[1];

            // Initialize socket
            socket = new DatagramSocket(nodePort);

            // Initialize cache control server
            cacheControlServer = new ControlServer();
            cacheControlServer.initialize();

            // Set up cache cooperation router
            InetAddress primaryServerIp = InetAddress.getByName(primaryServerAddress.split(":")[0]);
            int primaryServerPort = Integer.parseInt(primaryServerAddress.split(":")[1]);
            primaryServer = new Address();
            primaryServer.ipAddress = primaryServerIp;
            primaryServer.port = primaryServerPort;

            PIT = new HashMap<>();
            networkCacheSummary = new CountingBloomFilter(500, 5, Hash.MURMUR_HASH);
            totalReq = 0;

            CacheRouter router = new CacheRouter();
            System.out.println("Cache Control Router started on port: "+nodePort);
            router.acceptRequests();

        } catch (Exception e) {

            e.printStackTrace();
            System.exit(1);

        }
        finally {
            if(socket != null)
            socket.close();
        }

    }


}
