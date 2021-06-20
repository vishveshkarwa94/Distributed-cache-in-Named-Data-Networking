import java.net.DatagramSocket;
import java.net.InetAddress;

public class User {

    static DatagramSocket socket;
    static Address router;
    static int size;

    static void makeRequest(String dataName){
        Packet req = new Packet();
        req.setType("interest");
        req.setName(dataName);

    }

    public static void main(String[] args) {

        try{

            int port = Integer.parseInt(args[0]);
            String routerAddress = args[2];

            socket = new DatagramSocket(port);
            size = Integer.parseInt(args[1]);
            router = new Address();
            router.ipAddress = InetAddress.getByName(routerAddress.split(":")[0]);
            router.port = Integer.parseInt(routerAddress.split(":")[1]);

        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

}
