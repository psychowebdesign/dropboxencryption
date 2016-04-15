import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

public class Node {
	private ServerSocket socket;
	protected final static int BUFFER_SIZE = 1024;
	protected String CLIENT_TEMP_DIR = "./clientTemp/", TEMP_DIR = "./temp/";
	private String CUR_DIR = "/";
	protected final String ENCRYPTED_TAG = "encrypted_", DECRYPTED_TAG = "decrypted_";
	protected int port, errorCode;

	/**
	 * Removes substring from string
	 * @param name string from which to remove tag
	 * @param tag tag to remove from string
	 * @return
	 */
	protected String removeTag(String name, String tag){
		int startOfTag = name.indexOf(tag);
		if(startOfTag != -1){
			String base = name.substring(0, startOfTag);
			String remainder = name.substring((startOfTag+tag.length()), name.length());
			return base + "/" + remainder;
		}
		return "";
	}
	
	/**
	 * Get sub array from byte array
	 * @param array byte[]
	 * @param start start index
	 * @param end end index
	 * @return byte[]
	 */
	protected byte[] subArrayByte(byte[] array, int start, int end){
		assert(end>start);
		byte[] subArray = new byte[end-start];
		
		for(int i = 0, j=start; i<subArray.length;i++,j++){
			subArray[i] = array[j];
		}
		return subArray;
	}
	
	/**
	 * Combine to byte[]
	 * @param a prepending byte[]
	 * @param b byte[]
	 * @return byte[]
	 */
	protected byte[] prependByteArray(byte[] a, byte[] b){
		byte[] combined = new byte[a.length + b.length];
		int offset = 0;
		for(int i=0; i<a.length; i++){ 
			combined[i] = a[i]; 
			offset++;
		}
		for(int i=offset, j=0; i<b.length; i++, j++){ combined[i] = b[j]; }
		return combined;
	}
	
	/**
	 * Get string from byte[]
	 * @param buffer byte[]
	 * @param start int 
	 * @param end int
	 * @return String
	 */
	protected String byteArrayString(byte[] buffer, int start, int end){
		assert(end>=start);
		byte[] strBytes = new byte[end-start];
		for(int i=start, j=0; i<end;i++, j++){ strBytes[j] = buffer[i]; }
		String s = "";
		try {
			s =  new String(strBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) { e.printStackTrace(); }
		return s;
	}
	
	/**
	 * Returns last error code
	 * @return
	 */
	public int getErrorCode(){ return errorCode; }
	
	/**
	 * Get filename from path
	 * @param path
	 * @return String filename
	 */
	protected String getFileName(String path){
		String[] parts = path.split("/");
		return parts[parts.length-1];
	}
	
	/**
	 * Add substring to a string
	 * @param path string
	 * @param tag substring
	 * @return String
	 */
	protected String tagPathString(String path, String tag){
		int lastSlash = path.lastIndexOf("/");
		if(lastSlash != -1){
			String base = path.substring(0, lastSlash);
			String filename = path.substring(lastSlash+1, path.length());
			filename = tag + filename;
			return base + "/" + filename;
		}
		
		return "";
	}
	
	/**
	 * Resizes byte array. Adds in 0x00 when necessary. Prioritises start of array.
	 * @param arr byte[]
	 * @param newSize new size
	 * @return
	 */
	protected byte[] resizeByteArray(byte[] arr, int newSize){
		byte[] newArr = new byte[newSize];
		for(int i = 0; i < newArr.length; i++){
			newArr[i] = arr[i];
		}
		return newArr;
	}
	
	/**
	 * Insert String into byte array
	 * @param str String 
	 * @param arr byte[]
	 * @param offset int
	 * @return byte[]
	 */
	protected byte[] insertStringByteArray(String str, byte[] arr, int offset){
		byte[] strBytes = str.getBytes();
		for(int i=0, j = offset; i<str.length(); i++, j++)
			arr[j] = strBytes[i];
		
		return arr;
	}
}
