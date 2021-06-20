import org.apache.hadoop.util.bloom.CountingBloomFilter;
import org.apache.hadoop.util.bloom.Key;
import org.apache.hadoop.util.hash.Hash;

import java.util.HashMap;


public class ServerImpl implements Server{

    class Node {
        String key;
        byte[] value;
        Node next;
        Node previous = null;
    }

    private void addNode(Node node) {
        node.previous = head;
        node.next = head.next;
        head.next.previous = node;
        head.next = node;
    }

    private void removeNode(Node node){
        Node previous = node.previous;
        Node next = node.next;
        previous.next = next;
        next.previous = previous;
        bloomFilter.delete(new Key(node.key.getBytes()));
    }

    private void moveToHead(Node node){
        removeNode(node);
        addNode(node);
    }

    private Node popTail() {
        Node res = tail.previous;
        removeNode(res);
        return res;
    }

    private HashMap<String, Node> cache;
    private Node head;
    private Node tail;
    private int size;
    private CountingBloomFilter bloomFilter;



    @Override
    public boolean initialize(int initializeSize) {
        try {
            size = initializeSize;
            cache = new HashMap<>(size);
            head = new Node();
            tail = new Node();
            head.next = tail;
            tail.previous = head;
            bloomFilter = new CountingBloomFilter(20, 3, Hash.MURMUR_HASH);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }


    @Override
    public void insert(String key, byte[] value) {
        try{
            Node temp = new Node();
            temp.key = key;
            temp.value = value;
            bloomFilter.add(new Key(key.getBytes()));
            cache.put(key, temp);
            addNode(temp);
            if(cache.size() > size){
                tail = popTail();
                cache.remove(tail.key);
                bloomFilter.delete(new Key(tail.key.getBytes()));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean isPresent(String key) {
        return cache.containsKey(key);
    }

    @Override
    public byte[] get(String key) {
        if(!isPresent(key)) return null;
        Node temp = cache.get(key);
        moveToHead(temp);
        return temp.value;
    }

    @Override
    public CountingBloomFilter getBloomFilter() {
        return bloomFilter;
    }

}
