import java.net.InetAddress;
import java.util.Objects;

class Address{
    InetAddress ipAddress;
    int port;

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;


        if (o == null || this.getClass() != o.getClass()) return false;


        Address address = (Address) o;
        return this.port == address.port &&
                this.ipAddress.equals(address.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, port);
    }

}