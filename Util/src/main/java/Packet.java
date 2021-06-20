import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class Packet implements Serializable {
    private String type;
    private String name;
    private byte[] data;

    public Packet(){

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
