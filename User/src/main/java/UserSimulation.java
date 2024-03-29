import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.distribution.ZipfDistribution;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class UserSimulation{

    class UserThread extends Thread{

        int port;
        Address router;

        public UserThread(int port, Address router){
            this.port = port;
            this.router = router;
        }

        void makeRequests() throws IOException, InterruptedException {

            DatagramSocket socket = new DatagramSocket(port);
            Packet packet = new Packet();
            packet.setType("interest");

            for(int i = 0; i < numRequests;i++){
                Thread.sleep(100);
                String name = generator.sample()+".txt";
                packet.setName(name);
                byte[] buffer = new byte[65527];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                request.setAddress(router.ipAddress);
                request.setPort(router.port);
                request.setData(packet.serialize());
                socket.send(request);
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                socket.receive(reply);
                //Packet rep = SerializationUtils.deserialize(reply.getData());
                //if(!rep.getName().equals(name)) System.out.println(name+":"+rep.getName()+" ERROR :"+port);
            }
            socket.close();
            Thread.sleep(100);
            socket = new DatagramSocket(port+10);
            Packet interest_packet = new Packet();
            interest_packet.setType("getElements");
            byte[] buffer = new byte[65527];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            request.setAddress(router.ipAddress);
            request.setPort(router.port);
            request.setData(interest_packet.serialize());
            socket.send(request);
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            socket.receive(reply);
            Packet rep = SerializationUtils.deserialize(reply.getData());
            String data = new String(rep.getData());
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> list = new Gson().fromJson(data, listType);
            cacheElements.addAll(list);
            socket.close();
        }

        public void run(){
            try {
                makeRequests();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    static int numRequests = 100;
    static ZipfDistribution generator;
    static Set<String> cacheElements;
    static ArrayList<Address> routers;
    static int startPort;
    static Address serverAddress;
    static String type;

    private int getServerHits() throws IOException {
        DatagramSocket socket = new DatagramSocket(startPort);
        Packet packet = new Packet();
        packet.setName("getHits");
        packet.setType("getHits");
        byte[] buffer = new byte[65527];
        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
        request.setAddress(serverAddress.ipAddress);
        request.setPort(serverAddress.port);
        request.setData(packet.serialize());
        socket.send(request);
        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
        socket.receive(reply);
        socket.close();
        ByteArrayInputStream byteIn = new ByteArrayInputStream(reply.getData());
        DataInputStream dataIn = new DataInputStream(byteIn);
        return dataIn.readInt();

    }

    private void makeThreads(){
        UserThread[] threads = new UserThread[routers.size()];
        for(int i = 0; i < threads.length; i++)
        {
            threads[i] = new UserThread(startPort+i, routers.get(i));
        }
        for(int i = 0; i < threads.length; i++)
        {
            threads[i].start();
        }

        boolean isAlive = true;

        while (isAlive) {

            isAlive = false;

            for(int i = 0; i < threads.length; i++) {
                isAlive = ( isAlive || threads[i].isAlive());
            }

        }
    }

    private void initialSetUp(int numReq, double ex, int numContents) throws IOException {
        int tempNum = numRequests;
        numRequests = numReq;
        generator = new ZipfDistribution(numContents,ex);
        cacheElements = new HashSet();
        makeThreads();
        getServerHits();
        numRequests = tempNum;
    }

    private void processZipf() throws IOException {

        System.out.println("************* Test : Variable Zipf || Catalog size: 500 *************");
        ArrayList<Double> exponents = new ArrayList<>();
        exponents.add(0.5);
        exponents.add(0.6);
        exponents.add(0.7);
        exponents.add(0.8);
        exponents.add(0.9);

        for (Double ex : exponents) {
            initialSetUp(500, ex, 500);
            generator = new ZipfDistribution(500,ex);
            cacheElements = new HashSet();
            makeThreads();
            int serverHits = getServerHits();
            int totalReq = numRequests*routers.size();
            int cacheHit = totalReq - serverHits;
            double cacheHitRatio = ((double)cacheHit/totalReq)*100;
            System.out.println("Zipf paramater : "+ex+" | Cache Hit Ratio : " + cacheHitRatio);
        }
        System.out.println("************* Test End *************");
    }


    private void processNumContents() throws IOException {

        System.out.println("************* Test : Variable Number of Contents || Zipf Parameter: 0.7 *************");
        ArrayList<Integer> numContents = new ArrayList<>();
        numContents.add(200);
        numContents.add(300);
        numContents.add(400);
        numContents.add(500);
        initialSetUp(500, 0.7, 200);
        for (Integer nc : numContents) {
            initialSetUp(500, 0.7, nc);
            generator = new ZipfDistribution(nc,0.7);
            cacheElements = new HashSet();
            makeThreads();
            int serverHits = getServerHits();
            int cacheDiversity = cacheElements.size();
            int totalReq = numRequests*routers.size();
            int cacheHit = totalReq - serverHits;
            double cacheHitRatio = ((double)cacheHit/totalReq)*100;
            double diversityRatio = ((double) cacheDiversity/nc) *100;
            System.out.println("Number of Contents : "+nc+" | Cache Hit Ratio : " + cacheHitRatio+
                    " | Diversity Ratio : "+diversityRatio);
        }
        System.out.println("************* Test End *************");
    }



    private void start() throws IOException {
        if(type.equals("zipf")) processZipf();
        else processNumContents();
    }

    public static void main(String[] args) {
        try{
            if(args.length < 3) throw new Exception();

            startPort = Integer.parseInt(args[0]);
            numRequests = Integer.parseInt(args[1]);
            type = args[2];
            String routerAddresses = args[3];
            String serverAddressString = args[4];

            routers = new ArrayList<>();
            for(String routerAddress : routerAddresses.split(",")){
                Address router = new Address();
                router.ipAddress = InetAddress.getByName(routerAddress.split(":")[0]);
                router.port = Integer.parseInt(routerAddress.split(":")[1]);
                routers.add(router);
            }
            serverAddress = new Address();
            serverAddress.ipAddress = InetAddress.getByName(serverAddressString.split(":")[0]);
            serverAddress.port = Integer.parseInt(serverAddressString.split(":")[1]);

            UserSimulation simulation = new UserSimulation();
            simulation.start();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
