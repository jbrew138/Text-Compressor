import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.regex.Pattern;

/*
 * TCSS 342 - Spring 2015
 * Assignment 4 - Compressed Literature 2
 */

/**
 * @author James Brewer
 * @version A
 * 
 * Tests and uses Huffman's encoding algorithm, but uses a custom hash table for code/count
 * storage.
 * 
 * "testfile.txt" and a text file of choice (default "TheTimeMachine.txt") need to be included
 * in the project/default folder.
 *
 */
public class Main {
    
    private static int encodeSize;
    private static int decodeSize;
    private static long decodeTotalTime = 0;

    /**
     * @param args
     */
    public static void main(String[] args) {
        
//        //Test with a short test file first
        testEncoding();
        
        //Reset values after test
        decodeTotalTime = 0;
        encodeSize = 0;
        decodeSize = 0;
//        
        long totalEncodeTime = 0;
        
//        String filename = "testfile";
//        String filename = "TheTimeMachine";
        String filename = "WarAndPeace";
        
        System.out.println("Runtime and Compression statistics for encoding/decoding \""
                        + filename + ".txt\"\n");
        
        /*
         * Read file
         */
        long startTime = System.currentTimeMillis();
        String text = readFile(filename + ".txt"); //Import text as a string
        int textSize = text.length(); //Subtract 1 due to end character
        long stopTime = System.currentTimeMillis();
        
        totalEncodeTime += stopTime-startTime;
        
        //Print file runtime
        System.out.println("Read file Runtime:               " + (stopTime - startTime) + " ms");
        
        /*
         * Encode/Write data
         */
        
        ///// Create new tree based on text
        startTime = System.currentTimeMillis();
        CodingTree compressCode = new CodingTree(text); 
        stopTime = System.currentTimeMillis();
        
        totalEncodeTime += stopTime-startTime;
        System.out.println("Generate Code Runtime:           " + (stopTime - startTime) + " ms");
        
        //compressCode.displayCount();
        
        
        ///// Write to binary file
        startTime = System.currentTimeMillis();
        encodeFile(text, filename + "-encoded.txt", compressCode);
        stopTime = System.currentTimeMillis();
        
        totalEncodeTime += stopTime-startTime;
        
        //Print runtime for encoding
        System.out.println("Encode and Write Runtime:        " + (stopTime - startTime) + " ms");
        
        System.out.println("Total Read/Encode/Write Runtime: " + totalEncodeTime + " ms\n");
        
        compressCode.counts.stats(); //Display the stats of the hash table
        System.out.println();
        
        /*
         * Write code list
         */
        
        writeFile(compressCode.displayCodes(true), filename + "-codeGenerated");     
        
        /*
         * Calculate compression
         */
        
        //Calculate and print compression ratio of files (round to 2 decimal places)
        double compressRatio = (double) Math.round((double) encodeSize/textSize * 10000) / 100;
        System.out.println("Compression ratio: " + compressRatio + "%");
        System.out.println(encodeSize*8 + " bits (Encoded) vs. " + textSize*8 + " bits (Original)\n");
        
        /*
         * Decode and Write file
         */
        
        decodeFile(filename, compressCode, true); //Decode the encoded file

        //Print runtime for decoding
        System.out.println("Total Read/Decode/Write Runtime:        " + (decodeTotalTime) + " ms\n");
        
        System.out.println("Decompression ratio: " + ((double) decodeSize/textSize * 100) + "%");
    }
    
    //Reads the text file and converts it to a string
    public static String readFile(String filename) {
        StringBuilder textBuffer = new StringBuilder();
        String textImport = "";
        
        try {
            File file = new File(filename);
            Scanner fileRead = new Scanner(file);
            
            while (fileRead.hasNextLine()) {

                textBuffer.append(fileRead.nextLine()); //Read the line
                textBuffer.append("\r\n"); //Append with CR and LF characters (Windows text
                                                                            //environment)
                
            }
            
            textImport = textBuffer.toString(); //Convert the string buffer to regular string
            fileRead.close(); //Close the scanner
            
            return textImport;
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return textImport;
    }

    //Encode the text to a file given the codes
    public static void encodeFile(String text, String filename, CodingTree encoder) {
        try {
            
            //Data output used for writing bytes to file
            DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));
            
            String code = encoder.encode(text);

            int codeLength = code.length(); //Get length of binary string
            
            byte[] byteData = new byte[(codeLength/8) + 1]; //Create byte array from size of 
                                                            //binary string in "bytes"
            
            //Custom binary string to bytecode converter
            byte nextByte;
            String strByte;
            int length;
            
            for (int i = 0; i < codeLength; i += 8) { //Convert binary string byte to actual byte
                
                nextByte = 0; //Reset byte
                
                if ((codeLength - (i+8)) >= 0 ) { //
                    strByte = code.substring(i, i + 8); //Get the next byte
                    length = 7;
                } else { //Last section of binary code may not be a full byte
                    length = codeLength - i; //Change length accordingly
                    strByte = code.substring(i, i + length);
                    for (int k = 0; k < (8 - length); k++) {
                        strByte += '0';
                    }

                }
                
                //For each "bit" in the string byte, add it's binary value to the byte
                for(int j = 0; j <= length; j++) {
                    nextByte += (byte) (((strByte.charAt(j) - '0') & 0xff) << (7-j));
                }

                byteData[i/8] = nextByte; //Add the byte to the byte array
            }
            
            
            encodeSize = byteData.length; //Get the byte/char size of the encoded text
            
            out.write(byteData); //Write the entire byte array to the file

            out.close(); //Close the data output stream
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String decodeFile(String filename, CodingTree encoder, boolean Runtime) {
        StringBuilder binarySB = new StringBuilder();
        String binaryStr = "";
        MyHashTable<String, String> codes = new MyHashTable<String, String>(32768); //Map to be created from code file
        
        long startTime = System.currentTimeMillis();
        try {
            File file = new File(filename + "-encoded.txt");
            FileInputStream fileIn = new FileInputStream(file); //File reader for byte data
            int length = (int) file.length();
            int i = length;
            boolean lastBit = false;
            
            byte[] byteData = new byte[length]; //Create a byte array the length of the file 
            fileIn.read(byteData); //Read the file data into the byte array

            for (byte bt: byteData) {
                if (i <= 1) lastBit = true;
                binarySB.append(byteToBinary(bt,lastBit)); //Append each byte as a binary substring
                i--;
            }
            
            binaryStr = binarySB.toString();
            
            fileIn.close(); //Close the file input
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        
        decodeTotalTime += stopTime-startTime;
        //Print file runtime if flagged
        if (Runtime) System.out.println("Read Binary File and Translate Runtime: " + (stopTime - startTime) + " ms");
        
        startTime = System.currentTimeMillis();
        try {
            File fileCode = new File(filename + "-codeGenerated.txt"); //Open the generated codes file
            Scanner scan = new Scanner(fileCode);
            
            String line;
            String word = "";
            String code = "";
            
            scan.useDelimiter(Pattern.compile(",\\s|\\{|\\}")); //Split by ", ", "{", or "{"
            while(scan.hasNext()) {
                line = scan.next(); //Take the next token                
                if(!line.isEmpty()) { //Don't take the last token, as it's an empty string
                    for (int i = 0; i < line.length(); i++) {
                        if (line.charAt(i) == '=') {
                            word = line.substring(0, i);
                            code = line.substring(i+1);
                        }
                    }
                    codes.put(word, code);
                }
            }
            
            scan.close(); //Close the file input
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopTime = System.currentTimeMillis();
        
        decodeTotalTime += stopTime-startTime;
        //Print file runtime if flagged
        if (Runtime) System.out.println("Read Codes file:                        " + (stopTime - startTime) + " ms");
        
        
        startTime = System.currentTimeMillis();
        //Decode the binary string using the method in the encoder
        //encoder.codes is provided as in the guidelines, but goes unused
        String message = encoder.decode(binaryStr, codes);
        stopTime = System.currentTimeMillis();
        
        decodeTotalTime += stopTime-startTime;
        //Print file runtime if flagged
        if (Runtime) System.out.println("Decode Data Runtime:                    " + (stopTime - startTime) + " ms");
        
        startTime = System.currentTimeMillis();
        writeFile(message, filename + "-decoded"); //Write the decoded text to a different file
        stopTime = System.currentTimeMillis();
        
        decodeSize = message.length(); //Get the size of the decoded text for decompression stat
        
        decodeTotalTime += stopTime-startTime;
        //Print file runtime if flagged
        if (Runtime) System.out.println("Write Decoded File Runtime:             " + (stopTime - startTime) + " ms");
        
        return message;
    }
    
    //Simply writes a string to a text file
    public static void writeFile(String message, String filename) {
        try {
            File file = new File(filename+".txt");
            PrintWriter write = new PrintWriter(file);
            
            write.print(message); //Print the entire message to the file
            
            write.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Conversion of a byte to string binary
    public static String byteToBinary(Byte b, boolean lastBit) {
        
        /*
         * Solution from:
         * http://stackoverflow.com/questions/12310017/how-to-convert-a-byte-to-its-binary-string-representation
         * 
         */
        
        byte[] masks = { -128, 64, 32, 16, 8, 4, 2, 1 };
        StringBuilder binaryString = new StringBuilder();
        for (byte m : masks) {
            if ((b & m) == m) {
                binaryString.append('1');
            } else {
                binaryString.append('0');
            }
        }
        return binaryString.toString();
    }
    
    public static void testEncoding() {
        String filename = "testfile";
        System.out.println("Runtime and Compression statistics for encoding/decoding \""
                        + filename + ".txt\"\n");
        
        long startTime = System.currentTimeMillis();
        String text = readFile(filename + ".txt");

        int textSize = text.length(); //Subtract 1 due to extra end character
        CodingTree testCode = new CodingTree(text);
        System.out.println("Codes:\n" + testCode.displayCodes(false) + "\n");
        writeFile(testCode.displayCodes(true), filename + "-codeGenerated");
        System.out.println(testCode.displayCount());
        System.out.println("ORIGINAL:\n" + text);
        encodeFile(text, filename + "-encoded.txt", testCode);
        String decoded = decodeFile(filename, testCode, false);
        System.out.println("DECODED FROM COMPRESSION:\n" + decoded);
        long stopTime = System.currentTimeMillis();
        
        System.out.println("TEST RUNTIME: " + (stopTime - startTime) + " ms\n");
        //Calculate and print compression ratio of files (round to 2 decimal places)
        double compressRatio = (double) Math.round((double) encodeSize/textSize * 10000) / 100;
        System.out.println("Test Compression ratio: " + compressRatio + "%\n");
        System.out.println("Test Decompression ratio: " + ((double) decodeSize/textSize * 100)
                           + "% (If ratio is anything other than 100.0% then process fails)\n\n"
                           + "TEST END\n");
    }
}
