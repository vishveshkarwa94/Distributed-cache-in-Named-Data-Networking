import org.apache.commons.lang3.SerializationUtils;
import org.apache.hadoop.util.bloom.CountingBloomFilter;
import org.apache.hadoop.util.bloom.Key;
import org.apache.hadoop.util.hash.Hash;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;


public class Router {

    class HandleInterest implements Runnable{

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

        private Address getFaces(String key) {
            try{
                for(Map.Entry<Address, CountingBloomFilter> entry : cacheSummaries.entrySet()){
                    if(entry.getValue().membershipTest(new Key(key.getBytes()))){
                        return entry.getKey();
                    }
                }
                return null;
            }
            catch (Exception e){
                return null;
            }

        }

        private void sendData(Packet data, Address destination) throws IOException {
            byte[] buffer = new byte[65527];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            datagramPacket.setAddress(destination.ipAddress);
            datagramPacket.setPort(destination.port);
            datagramPacket.setData(data.serialize());
            socket.send(datagramPacket);
        }

        public void run() {

            String key = interest.getName();
            synchronized (cacheServer){
                if(cacheServer.isPresent(key)){
                    try {
                        Packet data = new Packet();
                        data.setName(interest.getName());
                        data.setType("data");
                        data.setData(cacheServer.get(key));
                        sendData(data, source);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    HashSet<Address> temp = PIT.get(key);
                    if(temp == null){
                        temp = new HashSet<>();
                    }
                    temp.add(source);
                    PIT.put(key, temp);
                    Address matchingFace = getFaces(key);
                    if(matchingFace == null || temp.contains(matchingFace)){
                        try {
                            forwardRequest(cacheCooperationRouter);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            forwardRequest(matchingFace);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    class HandleData implements Runnable{

        Packet data;
        Address source;

        HandleData(Packet data, Address source){
            this.data = data;
            this.source = source;
        }

        private void send(Packet data, Address destination) throws IOException {
            byte[] buffer = new byte[65527];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            datagramPacket.setAddress(destination.ipAddress);
            datagramPacket.setPort(destination.port);
            datagramPacket.setData(data.serialize());
            socket.send(datagramPacket);
        }

        public void run(){

            HashSet<Address> entryList = PIT.get(data.getName());
            if(entryList!= null && entryList.size() > 0){
                for(Address entry : entryList){
                    try {
                        send(data, entry);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(source.ipAddress.equals(cacheCooperationRouter.ipAddress) && source.port == cacheCooperationRouter.port){

                    synchronized (cacheServer){

                        String removed = cacheServer.insert(data.getName(), data.getData());
                        localCacheSummary = cacheServer.getBloomFilter();
                        if(removed != null){
                            Packet update = new Packet();
                            update.setType("deleteCache");
                            update.setName(removed);
                            try {
                                send(update, cacheCooperationRouter);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                    try {
                        sendSummaryUpdate();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                PIT.remove(data.getName());
            }
        }


    }

    static Server cacheServer;
    static HashMap<String, HashSet<Address>> PIT;
    static HashSet<Address> nearestNeighbours;
    static Address cacheCooperationRouter;
    static HashMap<Address, CountingBloomFilter> cacheSummaries;
    static CountingBloomFilter localCacheSummary;
    static DatagramSocket socket;

    public static byte[] serializeBloomFilter() throws IOException {
        java.io.ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);
        localCacheSummary.write(dataOutput);
        return outputStream.toByteArray();
    }

    public static CountingBloomFilter deSerializeBloomFilter(byte[] data) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(data);
        DataInput dataInput = new DataInputStream(inputStream);
        CountingBloomFilter temp = new CountingBloomFilter(cacheServer.getSize()*100, 3, Hash.MURMUR_HASH);
        temp.readFields(dataInput);
        return temp;
    }

    private static void sendSummaryUpdate() throws IOException {
        Packet update = new Packet();
        update.setType("summary");
        update.setData(serializeBloomFilter());
        for(Address router : nearestNeighbours){
            byte[] buffer = new byte[65527];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            datagramPacket.setAddress(router.ipAddress);
            datagramPacket.setPort(router.port);
            datagramPacket.setData(update.serialize());
            socket.send(datagramPacket);
        }
    }

    private static void sendCacheDetails(Address source) throws IOException {
        Packet packet = new Packet();
        packet.setType("cache");
        packet.setData(cacheServer.getElements());
        byte[] buffer = new byte[65527];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        datagramPacket.setPort(source.port);
        datagramPacket.setAddress(source.ipAddress);
        datagramPacket.setData(packet.serialize());
        socket.send(datagramPacket);
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
                switch (incomingPacket.getType()){
                    case "interest":
                        System.out.println("Received request type : "+incomingPacket.getType()+ " | name : "+incomingPacket.getName());
                        new HandleInterest(incomingPacket, source).run();
                        break;

                    case "data":
                        System.out.println("Received request type : "+incomingPacket.getType()+ " | name : "+incomingPacket.getName());
                        new HandleData(incomingPacket, source).run();
                        break;

                    case "summary":
                        System.out.println("Received request type : "+incomingPacket.getType());
                        cacheSummaries.put(source, deSerializeBloomFilter(incomingPacket.getData()));
                        break;

                    case "getElements":
                        System.out.println("Received request type : "+incomingPacket.getType());
                        sendCacheDetails(source);
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

            if(args.length < 3) throw new Exception();

            int nodePort = Integer.parseInt(args[0]);
            int cacheSize = 50;//Integer.parseInt(args[1]);
            String cacheCooperationRouterAddress = args[2];


            // Initialize socket
            socket = new DatagramSocket(nodePort);

            // Initialize cache server
            cacheServer = new ServerImpl();
            cacheServer.initialize(cacheSize);

            // Set up cache cooperation router
            InetAddress cacheCooperationRouterIp = InetAddress.getByName(cacheCooperationRouterAddress.split(":")[0]);
            int cacheCooperationRouterPort = Integer.parseInt(cacheCooperationRouterAddress.split(":")[1]);
            cacheCooperationRouter = new Address();
            cacheCooperationRouter.ipAddress = cacheCooperationRouterIp;
            cacheCooperationRouter.port = cacheCooperationRouterPort;

            PIT = new HashMap<>();
            cacheSummaries = new HashMap<>();
            localCacheSummary = cacheServer.getBloomFilter();

            // Set up neighbouring node list
            nearestNeighbours = new HashSet<>();
            if(args.length == 4){
                String neighbouringAddresses = args[3];
                String[] addresses = neighbouringAddresses.split(",");
                for(String address : addresses){
                    InetAddress ip = InetAddress.getByName(address.split(":")[0]);
                    int port = Integer.parseInt(address.split(":")[1]);
                    Address temp = new Address();
                    temp.ipAddress = ip;
                    temp.port = port;
                    nearestNeighbours.add(temp);
                }
            }

            Router router = new Router();
            System.out.println("Router Started on port: "+nodePort);
            sendSummaryUpdate();
            router.acceptRequests();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
