import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/*
 * TCSS 342 - Spring 2015
 * Assignment 3 - Compressed Literature
 */

/**
 * Huffman encoding tree class
 * 
 * @author James Brewer
 * @version A
 *
 */
public class CodingTree {
    
    public String bits;
    
    //Stores codes per word
    public MyHashTable<String, String> codes = new MyHashTable<String, String>(32768);
    
    //Stores the count of each word
    public MyHashTable<String, Integer> counts = new MyHashTable<String, Integer>(32768);
    
    //Holds the list of words found in the text
    private List<String> stringInclude;
    
    //Last word given in the message, to be used as a stopping point in decoding
    private String lastWord;

    //Create a new Huffman code tree given a message
    public CodingTree(String message) {
        WordCounter(message); //Count the characters and force special "end" character
        createTrees(); //Create the code tree
    }
    
    /*
     * Counting and organization of words
     */
    
    //Count the words in the message
    private void WordCounter(String message) {
        int length = message.length();        
        StringBuilder curWord = new StringBuilder();
        char curChar;
        
        for (int k = 0; k < length; k++) {
            //Get the next character
            curChar = message.charAt(k);
            
            //If the current character is accepted in a word, append it to the current word
            if ((('A' <= curChar) && (curChar <= 'Z'))
                            || (('a' <= curChar) && (curChar <= 'z'))
                            || (('0' <= curChar) && (curChar <= '9'))
                            || curChar == '\'' || curChar == '-') {
                curWord.append(curChar);
            } else {
                
                //Check if the string builder isn't empty (meaning it contains a full word)
                if (curWord.length() > 0) {
                    if (!counts.containsKey(curWord.toString())) { //If it doesn't already contain the word
                        counts.put(curWord.toString(), 1); //Add a new key/value pair
                    } else { // Otherwise increment existing pair
                        counts.put(curWord.toString(), counts.get(curWord.toString()) + 1);
                    }
                }
                
                curWord.setLength(0); //Reset the string builder
                curWord.append(curChar);
                //Add the character
                if (!counts.containsKey(curWord.toString())) {
                    counts.put(curWord.toString(), 1); //Add a new key/value pair
                } else {
                    //Increment existing pair
                    counts.put(curWord.toString(), counts.get(curWord.toString()) + 1);
                }
                if (k < length - 1) curWord.setLength(0); //Reset the builder
            }
        }
        
        //For the last word if not null
        if (curWord.length() != 0) {
            if (!counts.containsKey(curWord.toString())) {
                counts.put(curWord.toString(), 1); //Add a new key/value pair
            } else {
                //Increment existing pair
                counts.put(curWord.toString(), counts.get(curWord.toString()) + 1);
            }
        }
        
        //Create a different list from the available words for easy access in other methods
        stringInclude = new ArrayList<String>();
        stringInclude.addAll(counts.keySet());
        
        lastWord = curWord.toString(); //Retrieve the last character
    }
    
    //Sort the list of nodes by weight. Collections.sort() gives fastest runtime
    private void sortTreeWeight(List<Node> nodeList) {
        Collections.sort(nodeList);
    }

    /*
     * Creation and coding of trees
     */
    
    //Begin creating the coding trees
    private void createTrees() {
        //ArrayList works 4x faster than LinkedList here (War/Peace test: ~3 sec vs ~12 sec)
        List<Node> trees = new ArrayList<Node>(); //Initialize list for nodes
        
        //For each word included add a new node with weight based on count
        for(String word : stringInclude) {
            trees.add(new Node(word, null, null, counts.get(word)));
        }
        sortTreeWeight(trees); //Sort the nodes by weight, lowest first

        
        System.out.print("Huffman Coding process: [");

        //While there are multiple nodes/trees left, merge them
        while (trees.size() > 1) {

            //Add a new empty node (null character) with the lowest weighted nodes/trees as
            //children, and add weights together
            trees.add(new Node("", trees.get(0), trees.get(1),
                               trees.get(0).weight + trees.get(1).weight));
            trees.remove(0); //Remove the first two entries
            trees.remove(0);
            sortTreeWeight(trees); //Sort the trees again
            if ((stringInclude.size() >= 20) && (trees.size() % (stringInclude.size()/20) == 0)) {
                System.out.print("=");
            }
        }
        System.out.println("]");
        
        trees.get(0).generateCodes(""); //Start empty and let paths fill out on leaves
        
    }
    
    //Print the word counts to console (Testing only)
    public String displayCount() {
        StringBuilder result = new StringBuilder();
        result.append("Character count:\n");
        for(String word: stringInclude) {
            result.append(counts.get(word) + " of " + word + "\n");
        }
        
        return result.toString();
    }
    
    //Print the generated word codes to a string
    public String displayCodes(boolean toFile) {
        StringBuilder result = new StringBuilder();
        int lines = 0;
        
        result.append("{");
        //Iterate through all included words
        for (String word: stringInclude) {
            result.append(word + "=" + codes.get(word) + ", ");
            //If being displayed in console, create new lines
            if (!toFile && (result.length())/90 > lines) {
                lines++;
                result.append("\n");
            }
        }
        result.setLength(result.length() - 2);
        result.append("}");
        
        return result.toString();
    }
    
    //Encode the original text given to the constructor of the code tree
//    public String encodeOriginal() {
//        StringBuilder code = new StringBuilder();
//        
//        for(String word : messageList) {
//            code.append(codes.get(word));
//        }     
//        
//        return code.toString();
//    }
    
    //Encode a given string, may or may not be original text code tree was based off of
    //Repeats code used in the original word counter, but adds words to a list that is then
    //converted to code
    public String encode(String message) {
        StringBuilder code = new StringBuilder();

        int length = message.length();        
        StringBuilder curWord = new StringBuilder();
        char curChar;
        
        curWord.append(message.charAt(0));
        
        for (int k = 1; k < length; k++) {
            curChar = message.charAt(k);
            
            if ((('A' <= curChar) && (curChar <= 'Z'))
                            || (('a' <= curChar) && (curChar <= 'z'))
                            || (('0' <= curChar) && (curChar <= '9'))
                            || curChar == '\'' || curChar == '-') {
                curWord.append(curChar);
            } else {
                
                //Check if a character was just sent last, meaning the string builder is still empty
                // If not, then encode the word and append it to the binary string
                if (curWord.length() > 0) {                  
                    code.append(codes.get(curWord.toString()));
                }
                
                curWord.setLength(0); //Reset the string builder
                curWord.append(curChar);
                //Convert the character to it's code and append it to the binary string
                code.append(codes.get(curWord.toString()));
                curWord.setLength(0);
                
            }
        }
        return code.toString();
    }

    //Decode the binary string given
    public String decode(String bits, MyHashTable<String, String> codes) {
        
        //Create a reverse hash table for decoding, taken from the codes table
        //This allows lookup by code
        MyHashTable<String, String> decodes = new MyHashTable<String, String>(32768);
        
        //Convert given codes to a decode table
        List<String> decodeInclude = new ArrayList<String>();
        decodeInclude.addAll(counts.keySet());
        for(String word: decodeInclude) {            
            decodes.put(codes.get(word), word);
        }
        
        StringBuilder code = new StringBuilder(8); //String builder for fast appending
        String codeWord;
        int length = bits.length();
        
        StringBuilder output = new StringBuilder(); //Builder for text output
        
        //Loop through the bits searching for words
        for(int i = 0; i < length - 1; i++) {
            
            code.append(bits.charAt(i)); //Append the next bit
            codeWord = decodes.get(code.toString());
            
            if (codeWord != null) { //If the code provided a word
                output.append(codeWord); //Append the word to the text
                
                if ((i >= length - 8) && codeWord.equals(lastWord)) {
                    break; //End word reached within last byte, stop decoding
                }
                code.setLength(0); //Reset for next code
            }
        }
        
        return output.toString(); //Return the text as an entire string
    }
    
    /**
     * Node class for a Huffman tree structure
     * 
     * @author James Brewer
     * @version A
     *
     */
    private class Node implements Comparable<Object>{
        Node ln;
        Node rn;
        String value; //Character value in node
        int weight; //Weight of character count, or total subtree weight
        
        //Create a new node
        private Node(String theValue, Node left, Node right, int count) {
            value = theValue;
            ln = left;
            rn = right;
            weight = count;
        }
        
        //Recursive method to generate codes
        public void generateCodes(String path) {
            if(ln == null && rn == null) {
                //If both children are null, then the path given is the code for that character
                codes.put(value, path);
                return;
            }
            
            //Generate codes for the children if available
            if (ln != null) ln.generateCodes(path + '0');
            if (rn != null) rn.generateCodes(path + '1');
        }
        
        //Convert the node to a string (for testing purposes only)
        public String toString() {
            String left;
            String right;
            if (ln == null) {
                left = "";
            } else {
                left = ln.toString();
            }
            if (rn == null) {
                right = "";
            } else {
                right = rn.toString();
            }
            
            return "{" + value + ", " + weight + "}" + "["+ left + right + "]";            
        }

        @Override
        public int compareTo(Object o) {
            if (o.getClass() == Node.class) {
                return this.weight - ((Node) o).weight;
            }
            return 0;
        }
    

    }
}
