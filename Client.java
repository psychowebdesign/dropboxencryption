import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;

import com.sun.xml.internal.ws.util.StringUtils;

public class Client extends Node {
	
	private Socket socket;
	private String server;
	BufferOverflowByte bufferOverflow;
	private String CUR_DIR = "/";
	
	Client(String pserver, int pport){
		server = pserver;
		port = pport;
		errorCode = -1;
		bufferOverflow = new BufferOverflowByte();
	}
	
	/**
	 * Moves back one directory
	 * @return
	 */
	public boolean back(){
		if(CUR_DIR.length() > 1){
			String[] parts = CUR_DIR.split("/");
			for(String p : parts){
				System.out.println("p:" + p);
			}
			String newDir = "/";
			for(int i = 0; i < parts.length-1; i++){
				if(parts[i].length() > 0)
					newDir += "/" + parts[i];
			}
			CUR_DIR = newDir;
		}
		
		
		return true;
	}
	
	/**
	 * Changes current directory
	 * @param newDir name of new directory within current directory to change into
	 * @return
	 */
	public boolean cd(String newDir){
		CUR_DIR = CUR_DIR + "/" + newDir;
		return true;
	}
	
	/**
	 * Download a file from server
	 * @param path String path to dropbox file to download
	 * @return
	 */
	public boolean download(String path){
		//build get command
		String command = "GT:" + CUR_DIR + "/" + getFileName(path);
		try {
			//send command
			socket = new Socket(server, port);
			byte[] send = command.getBytes();
			OutputStream os = socket.getOutputStream();
			os.write(send, 0, send.length);
			os.flush();
			
			//get input stream, read first part of response for status code
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			InputStream in = socket.getInputStream();
			bytesRead = in.read(buffer);
			int statusCode = (int)buffer[0];
			
			//stop trying if command is not OK and return false
			if(statusCode != 0) { 
				this.errorCode = statusCode;
				socket.close();
				return false; 
			}
			
			//remove ENCRYPTED_TAG
			String cleanPath = removeTag(path, ENCRYPTED_TAG);
			
			//retrieve file
			String getPath = CLIENT_TEMP_DIR + getFileName(cleanPath);
			File tempPath = new File(getPath.trim());
			if(!tempPath.exists()) {
				System.out.println(tempPath.getAbsolutePath());
				tempPath.createNewFile();
			} 
			OutputStream output = new FileOutputStream(tempPath);
		    while ((bytesRead = in.read(buffer)) != -1) {
		        output.write(buffer, 0, bytesRead);
		    }
		    
		    // Closing the FileOutputStream handle
		    output.close();
	        socket.close();
			
		} catch (IOException e) { e.printStackTrace(); }
		
		return true;
	}
	
	/**
	 * Upload a file to dropbox
	 * @param path String local path to file to upload
	 * @return
	 */
	public boolean upload(String path){
		//build send command
		String command = "SD:" + CUR_DIR + "/" + getFileName(path);
		try {
			//send send command
			socket = new Socket(server, port);
			byte[] send = command.getBytes();
			OutputStream os = socket.getOutputStream();
			os.write(send, 0, send.length);
			os.flush();
			
			//get input stream, read first part of response for status code
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			InputStream in = socket.getInputStream();
			bytesRead = in.read(buffer);
			int statusCode = (int)buffer[0];
			
			//if response code not OK, stop
			if(statusCode != 0) { 
				this.errorCode = statusCode;
				socket.close();
				return false; 
			}
			
			//send file
			File myFile = new File(path);
	        byte[] mybytearray = new byte[(int) myFile.length()];
	        FileInputStream fis = new FileInputStream(myFile);
	        BufferedInputStream bis = new BufferedInputStream(fis);
	        bis.read(mybytearray, 0, mybytearray.length);	         
	        os.write(mybytearray, 0, mybytearray.length);
	        os.flush();
	         
	        socket.close();
			bis.close();
			
		} catch (IOException e) { e.printStackTrace(); }
		
		
		return true;
	}
	
	/**
	 * List CUR_DIR
	 * @return
	 */
	public Metadata[] ls(){
		return ls(CUR_DIR);
	}

	/**
	 * List specified path on dropbox
	 * @param path
	 * @return
	 */
	public Metadata[] ls(String path){
		ArrayList<Metadata> temp_metad = new ArrayList<Metadata>();
		String command = "LS:" + path;
		try {
			//connect to server and request a listing of the specified directory path
			socket = new Socket(server, port);
			byte[] send = command.getBytes();
			OutputStream os = socket.getOutputStream();
			os.write(send, 0, send.length);
			os.flush();
			//get input stream, read first part of response for status code
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			InputStream in = socket.getInputStream();
			bytesRead = in.read(buffer);
		
			int statusCode = (int)buffer[0];
			for(int di = 0; di < 10; di++){
				System.out.println("==> " + buffer[di]);
			}
			//buffer will start with response code byte, the ':'
			//remove these
		
			buffer = subArrayByte(buffer, 2, buffer.length);
			System.out.println("status code =="+statusCode);
			if(statusCode==0){
				//parse remaining buffer contents
				temp_metad.addAll(parseMetadata(buffer));
				while((bytesRead = in.read(buffer)) != -1){
					temp_metad.addAll(parseMetadata(buffer));
				}
				
				in.close();
				os.close();
			} else{
				errorCode = statusCode;
				in.close();
				os.close();
				socket.close();
				return null;
			} 
			
			os.close();
			in.close();
			socket.close();
		} catch (IOException e) { e.printStackTrace(); }
		
		
		Metadata[] return_metad = new Metadata[temp_metad.size()];
		return temp_metad.toArray(return_metad);
	}
	
	/**
	 * Creates an array of Metadata objects from the encoded byte stream
	 * Maintains a state between calls where any overflowing data that hasn't formed
	 * a Metadata object is saved in BufferOverflow
	 * @param data
	 * @return
	 */
	private ArrayList<Metadata> parseMetadata(byte[] data){
		ArrayList<Metadata> return_metad = new ArrayList<Metadata>();
		byte boundaryMark = ":".getBytes()[0];

		//if any overflow has been saved, prepend to current data
		if(bufferOverflow.set()){
			data = prependByteArray(bufferOverflow.get(), data);
			bufferOverflow.set(false);
		}
		
		byte flag = data[0];
		int sectionOffset = 1;
		//1st byte is flag for folder/file
		int i;
		
		//run through data and create Metadata objects as necessary
		//save to return_metad
		for(i = 1; i<data.length-2; i++){
			if(data[i] == boundaryMark){				
				//0x00 = file, 0x01 = folder
				boolean folder = (flag != 0x00);
				String path = byteArrayString(data, sectionOffset, i);
				String name = getFileName(path);
				return_metad.add(new Metadata(folder, name, path));
			
				//if end of stream detected, return Metadata list
				if(data[i+1] == boundaryMark){
					return return_metad;
				}
				
				flag = data[++i];
				sectionOffset = ++i;
			
			}
		}
		
		//there is info still in data, must save here
		bufferOverflow.save(data, sectionOffset-1, data.length);

		//return data
		return return_metad;
	}
	
	/**
	 * Convenience class - saves buffer overflow
	 */
	private class BufferOverflowByte{
		private byte[] overflow;
		private boolean isSet;
		
		BufferOverflowByte(){
			isSet = false;
		}
		
		public void save(byte[] array, int start, int end){
			byte[] temp = new byte[end-start];
			for(int i=start, j=0;i<end;i++,j++){
				temp[j] = array[i];
			}
			overflow = temp;
			isSet = true;
		}
		
		public boolean set(){
			return isSet;
		}
		
		public void set(boolean s){ isSet = s; }
		
		public byte[] get(){ return overflow; }
	}
}
