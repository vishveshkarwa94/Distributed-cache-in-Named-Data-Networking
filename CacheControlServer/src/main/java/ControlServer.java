import java.util.ArrayList;
import java.util.HashMap;

public class ControlServer {
    private HashMap<String, ArrayList<Address>> map;

    public void initialize(){
        map = new HashMap<String, ArrayList<Address>>();
    }

    public void add(String dataName, Address router){
        ArrayList temp = map.get(dataName);
        if(temp == null){
            temp = new ArrayList();
        }
        temp.add(router);
        map.put(dataName, temp);
    }

    public Address get(String dataName){
        if(!map.containsKey(dataName)) return null;
        ArrayList<Address> routers = map.get(dataName);
        Address res = routers.get(0);
        routers.remove(0);
        routers.add(res);
        return res;
    }

    public void remove(String dataName, Address router){
        if(!map.containsKey(dataName)) return;
        ArrayList<Address> routers = map.get(dataName);
        if(routers.contains(router)){
            routers.remove(router);
        }
        if(routers.size() == 0) map.remove(dataName);
    }

}
