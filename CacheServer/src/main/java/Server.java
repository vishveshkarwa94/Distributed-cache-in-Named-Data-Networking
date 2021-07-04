import org.apache.hadoop.util.bloom.CountingBloomFilter;

import java.util.Set;


public interface Server{
    public boolean initialize(int initializeSize);
    public String insert(String key, byte[] value);
    public boolean isPresent(String key);
    public byte[] get(String key);
    public CountingBloomFilter getBloomFilter();
    public int getSize();
    public byte[] getElements();
}