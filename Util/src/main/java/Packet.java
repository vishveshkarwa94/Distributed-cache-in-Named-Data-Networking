import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class Packet implements Serializable {
    private PacketTypes type;
    private String name;
    private byte[] data;

    public Packet(){
        type = null;
        name = null;
        data = null;
    }

    public String getType() {
        return type.name();
    }

    public void setType(String type) {
        switch (type){
            case "interest":
                this.type = PacketTypes.interest;
                break;

            case "data":
                this.type = PacketTypes.data;
                break;

            case "deleteCache":
                this.type = PacketTypes.deleteCache;
                break;

            case "summary":
                this.type = PacketTypes.summary;
                break;

            case "getElements":
                this.type = PacketTypes.getElements;
                break;

            case "getHits":
                this.type = PacketTypes.getHits;
                break;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] serialize(){
        return SerializationUtils.serialize(this);
    }

}
