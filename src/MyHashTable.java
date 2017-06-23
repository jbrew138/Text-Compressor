import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/*
 * TCSS 342 - Spring 2015
 * Assignment 4 - Compressed Literature 2
 */

/**
 * @author James Brewer
 * @version
 *
 */
public class MyHashTable<K, V> {
    
    //Contains all of the buckets
    private ArrayList<Bucket> buckets;
    
    //A set of all the keys to be returned if requested
    private Set<K> keys = new HashSet<K>();
    
    //The capacity given for the table
    private int capacity;
    
    //The histogram of how many probes each K/V pair takes to get to them
    private ArrayList<Integer> histogram = new ArrayList<Integer>();
    
    //On average, from a test of 5 trials from using steps 1 through 10
    //A step of 5 gave the best times in decoding, while affecting nothing else
    //other than the number of probes which had negligible difference
    private int probeStep = 5;
    
    //Creates a new hash table with the given capacity
    public MyHashTable (int capacity) {
        buckets = new ArrayList<Bucket>(capacity);
        for(int i = 0; i < capacity; i++) {
            buckets.add(null); //Add null values to fill the table index
        }
        
        histogram.add(0, 0); //Initialize the histogram
        
        this.capacity = capacity;
    }
    
    //Puts a value at the given key place
    public void put(K searchKey, V newValue) {
        int hash = hash(searchKey);
        
        //If the key is new and the location is empty, add a new bucket
        if(!containsKey(searchKey) && (buckets.get(hash) == null)) {
            buckets.set(hash, new Bucket(searchKey, newValue, hash));
            histoAdd(0); //Adding a new value with zero probes
        } else { //Otherwise find where it needs to go
            
            int i = 0;
            int probes = 0;
            
            
            while (buckets.get(hash + i) != null) {
                //If the key matches with the found location, update the value given
                if (buckets.get(hash + i).getKey().equals(searchKey)) {
                    buckets.get(hash + i).putValue(newValue);
                    return;
                }
                
                //Increment the offset index with the number of steps to take per probe
                i = i + probeStep;
                probes++; // Increment probe count
                
                //
                if ((hash + i) >= (capacity - 1)) { //gone past the table length
                    i = -hash; //Begin at the table's start 
                } else if (i >= (capacity - 1 - hash)) { //reached the original index start
                    return; //Return without adding anything, as there is no space for it
                }
            }
            //If the index landed on a later location that was null, add the new bucket
            buckets.set(hash + i, new Bucket(searchKey, newValue, hash + i));
            histoAdd(probes); //Adding a new value with one or more probes
        }
        
        //Add the new key to the key set
        keys.add(searchKey);


    }

    //Gets the value from the given key
    public V get(K searchKey) {
        int hash = hash(searchKey);
        
        if(buckets.get(hash) == null) { //Similar behavior to a Java HashMap, if empty, return null
            return null;
        }
        
        //If the key equals the key at the first hash, return that value
        if (searchKey.equals(buckets.get(hash).getKey())) {
            return buckets.get(hash).getValue();
        } else { //Otherwise it must be searched for
            
            int i = 0;

            //While the location isn't null and the key hasn't been found
            while (buckets.get(hash + i) != null && !searchKey.equals(buckets.get(hash + i).getKey())) {
                i = i + probeStep; //Increment the offset index

                //Same loop to beginning and ending cases as in put() method
                if ((hash + i) >= (capacity - 1)) {
                    i = -hash;
                } else if (i >= (capacity - 1 - hash)) {
                    return null;
                }
            }
            
            if (buckets.get(hash + i) == null) { //If empty, return null
                return null;
            }

            //Otherwise return the proper value if found
            return buckets.get(hash + i).getValue();
        }
    }
    
    //Checks the stored key set if the key is included
    public boolean containsKey(K searchKey) {
        return keys.contains(searchKey);
    }
    
    //Returns the set of only keys (Java HashMap behavior needed elsewhere)
    public Set<K> keySet() {
        return keys;
    }
    
    //Generates a hashcode based on the key
    private int hash(K key){
        int hashed;
        
        //If the key isn't null
        if (key != null) {
            hashed = key.hashCode() % capacity; //Generates a hash based on the range given
                                                //by the capacity (i.e. 0 to 32767)
        } else {
            return -1; //If the key is null, return -1
        }
        
        //If the mod gave a negative value, change it to it's positive counterpart
        if (hashed < 0) {
            return hashed + capacity;
        } else { //Otherwise simply return
            return hashed;
        }
    }
    
    //Prints the stats of the hash table
    public void stats() {
        int entries = entryCount();
        
        System.out.println("Hash Table Stats"
                        + "\n==================");
        System.out.println("Number of Entries: " + entries);
        System.out.println("Number of Buckets: " + capacity);
        System.out.println("Histogram of Probes: " + histoPrint());
        
        System.out.println("Fill Percentage: " + ((double) 100*entries/capacity) + "%");
        System.out.println("Max Linear Probe: " + histogram.size());
        System.out.println("Average Linear Probe: " + histoAvg());
    }
    
    //Counts the number of entries in the hash table
    private int entryCount() {
        return keys.size(); //Key set already contains the count
    }
    
    //Increment the count of the probe given
    private void histoAdd(int probes) {
        //If the number of probes goes beyond the list index, fill the list with empty counts
        //until it reaches the probe number
        if (probes >= histogram.size()) {
            for(int i = histogram.size(); i <= probes; i++) {
                histogram.add(i, 0);
            }
        }
        
        //Set the new value by incrementing
        histogram.set(probes, histogram.get(probes)+1);

    }
    
    //Print the histogram
    private String histoPrint(){
        StringBuilder result = new StringBuilder();
        int lines = 0;
        
        result.append("[");
        for (Integer p: histogram) {
            result.append(p + ", "); //Add the next value
            
            if ((21 + result.length())/75 > lines) { //Creates new line when needed
                lines++;
                result.append("\n");
            }
        }
        result.setLength(result.length() - 2); //Throw out the last ", "
        result.append("]");
        return result.toString();
    }
    
    //Find the average number of probes needed
    private double histoAvg(){
        double entryTotal = 0;
        double probesTotal = 0;
        
        for (int i = 0; i <= histogram.size() - 1; i++) {
            entryTotal += histogram.get(i); //Get the amount of entries
            probesTotal += (i+1)*histogram.get(i); //Count the total probes done by multiplying
        }
        
        //Calculate the average
        return probesTotal/entryTotal;
    }
    
    //Allows for the display of buckets in the table at their positions
    public String toString() {
        return buckets.toString();
    }
    
    /**
     * 
     * A bucket contains the key and value, and the actual index (for testing purposes)
     * Could potentially be expanded to hold multiple values later through chaining, but
     * for now only holds one value as requested.
     * 
     * @author James Brewer
     * @version A
     *
     */
    private class Bucket {
        
        private K key;
        private V value;
        private int actualIndex;
        
        //Create a new bucket from the K/V pair, and store the index
        private Bucket(K theKey, V theValue, int index) {
            key = theKey;
            value = theValue;
            actualIndex = index;
        }
        
        //Return the key in the bucket for searching purposes
        private K getKey(){
            return key;
        }
        
        //Get the value in the bucket
        private V getValue(){
            return value;
        }
        
        //Put a new value in the bucket
        private void putValue(V newValue) {
            value = newValue;
        }
        
        //Returns the display of the K/V pair and the location of the bucket in the table
        public String toString() {
            return "<" + key.toString() + ", " + value.toString() + " @ " + actualIndex + ">";
        }
        
    }
    
}
