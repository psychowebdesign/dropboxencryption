import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Node implements Runnable {

	private Thread serverThread;
	private ServerSocket socket;
	private Dropbox dropbox;
	private Encryptinator crypty;
	private final String ENCRYPTED_TAG = "encrypted_";
	private String[] whitelist;
	
	Server(int port, String[] newWhitelist){
		try {
			socket = new ServerSocket(port);
		} catch (IOException e) { e.printStackTrace(); }
		
		dropbox = new Dropbox();
		crypty = new Encryptinator();
		whitelist = newWhitelist;
	}
	/**
	 * Sets current whitelist addresses
	 * @param newWhitelist new String[] whitelist
	 */
	public synchronized void setWhitelist(String[] newWhitelist){
		whitelist = newWhitelist;
	}
	
	/**
	 * Start server thread
	 */
	public void start(){
		if(serverThread == null){
			serverThread = new Thread(this);
			serverThread.start();
		}
	}
	
	/**
	 * Main server thread method
	 */
	public void run(){
		System.out.println("server starting up");
		while(true){
			try {
				Socket clientSocket = socket.accept();
				handleClient(clientSocket);
				
				
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/**
	 * Is IP on whitelist
	 * @param IP String IP to search for
	 * @return true if IP on whitelist, false otherwise
	 */
	private boolean onWhitelist(String IP){
		for(String s : whitelist){
			if(s.equals(IP)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Handles a request from a client based on whether they're on whitelist and type of command
	 * @param clientSocket 
	 * @throws IOException
	 */
	private void handleClient(Socket clientSocket) throws IOException{
		InetAddress ia = clientSocket.getInetAddress();
		
		//check if on whitelist
		if(!onWhitelist(ia.getHostName().trim())){
			//send error code and return
			simpleResponse(clientSocket, (byte)0xff, true);
			System.out.println("denying access to " + ia.getHostAddress());
			return;
		}
		
		//firstly read command from client
		InputStream is = clientSocket.getInputStream();
		byte[] buffer = new byte[1024];
		is.read(buffer);
		String request = new String(buffer, "UTF-8");
		String[] requestParts = request.split(":");
		assert(requestParts.length == 2);
	
		//send command to one of command handlers
		switch(requestParts[0]){
			case "LS": //listing
				respondLS(clientSocket, requestParts[1]);
				break;
			case "GT": //download request
				respondGT(clientSocket, requestParts[1]);
				break;
				
			case "SD"://upload request
				respondSD(clientSocket, requestParts[1]);
				break;
				
			default:
				assert(false);
		}
	}
	
	/**
	 * Sends a simple status back to client
	 * @param s socket 
	 * @param b byte status
	 * @param close if true, socket will be closed before return. Otherwise, socket 
	 * left open
	 */
	private void simpleResponse(Socket s, byte b, boolean close){
		try {
			OutputStream os = s.getOutputStream();
			byte[] section = new byte[BUFFER_SIZE];
			section[0] = b;
			os.write(section, 0, BUFFER_SIZE);
			os.flush();
			if(close){ s.close(); }
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Upload File request handler. 
	 * File will be encrypted and renamed to include the ENCRYPT_TAG before being sent
	 * to dropbox.
	 * @param clientSocket
	 * @param filePath dropbox path to file to upload
	 */
	private void respondSD(Socket clientSocket, String filePath) {
		//respond OK to client
		simpleResponse(clientSocket, (byte)0x00, false);
		try {
			InputStream in = clientSocket.getInputStream();
			
			//create file in TEMP_DIR for upload
			String path = TEMP_DIR + getFileName(filePath);
			File tempPath = new File(path.trim());
			if(!tempPath.exists()) {
				System.out.println(tempPath.getAbsolutePath());
				tempPath.createNewFile();
			} 
			
			OutputStream output = new FileOutputStream(tempPath);
		    int bytesRead;
		    byte[] buffer = new byte[1024];
		    while ((bytesRead = in.read(buffer)) != -1) {
		        output.write(buffer, 0, bytesRead);
		    }
		    // Closing the FileOutputStream handle
		    output.close();
		    
		    //path to encrypted file
		    String encrpytedFilePath = tagPathString(path, ENCRYPTED_TAG);
		    
		    //dropbox path to upload file
		    String dropboxDestination = tagPathString(filePath.trim(), ENCRYPTED_TAG);
		    
		    //encrypt file and store in encrypted file path
		    crypty.encrypt(new File(path.trim()), new File(encrpytedFilePath.trim()));
		    
		    //upload to dropbox
		    dropbox.upload(encrpytedFilePath, dropboxDestination);
		    
		    //clean temp directory
		    tempPath.delete();
		    new File(encrpytedFilePath.trim()).delete();
		} catch (IOException e) { e.printStackTrace(); }
	}
	


	/**
	 * Download File request handler
	 * @param clientSocket
	 * @param pathToFile dropbox path to file to upload
	 */
	private void respondGT(Socket clientSocket, String pathToFile) {
		//repsond OK to client
		simpleResponse(clientSocket, (byte)0x00, false);

		try {
			
			//download file from dropbox
			OutputStream os = clientSocket.getOutputStream();
			String savePath = TEMP_DIR + getFileName(pathToFile);
			File tempPath = new File(savePath.trim());
			
			if(!tempPath.exists()){
				tempPath.createNewFile();
			}
			
			dropbox.download(pathToFile, savePath);
			
			//decrypt file if necessary
			if(savePath.contains(ENCRYPTED_TAG)){
				String newName = removeTag(savePath, ENCRYPTED_TAG);
				crypty.decrypt(new File(savePath.trim()), new File(newName.trim()));
				tempPath.delete();
				tempPath = new File(newName.trim());
			}
			
			//send file to client
	        byte[] mybytearray = new byte[(int) tempPath.length()];
	        FileInputStream fis = new FileInputStream(tempPath);
	        BufferedInputStream bis = new BufferedInputStream(fis);
	        bis.read(mybytearray, 0, mybytearray.length);	         
	        os.write(mybytearray, 0, mybytearray.length);
	        os.flush();
		    clientSocket.close();
		    
		    //clean temp directory
			tempPath.delete();
			bis.close();
		} catch (IOException e) { e.printStackTrace(); }

	}
	
	/**
	 * Listing request handler
	 * @param clientSocket
	 * @param path dropbox path to directory to list
	 */
	private void respondLS(Socket clientSocket, String path){
		//NOTE - actual data is packed into first stream buffer
		try {
			Metadata[] listing = dropbox.ls(path.trim());
			//encode Metadata listing into byte array
			byte[] toSend = packageToSend(listing, BUFFER_SIZE*BUFFER_SIZE);
			OutputStream os = clientSocket.getOutputStream();
			byte[] section = new byte[BUFFER_SIZE];
			
			//pack toSend byte array into buffer and send
			for(int i = 0, j=0; i < toSend.length; i++, j++){
				if( (i != 0) && ( (i%BUFFER_SIZE) == 0) ){
					os.write(section, 0, BUFFER_SIZE);
					j = 0;
				}
				
				section[j] = toSend[i];
			}
		
			//send remaining
			os.write(section, 0, BUFFER_SIZE);
			os.close();
			
		} catch (IOException e) { e.printStackTrace(); }
	}


	/**
	 * Encodes a list of Metadata into a byte array conforming to standard outlined
	 * in report
	 * @param data Metadata array to encode
	 * @param bufferSize size of buffer to fit
	 * @return encoded byte array representing the Metadata
	 */
	public byte[] packageToSend(Metadata[] data, int bufferSize){
		byte[] buffer = new byte[bufferSize];
		int currentOffset = 0;
		buffer[currentOffset++] = (byte) 0x00;
		
		for(Metadata m : data){
			if( (currentOffset + m.path().length() + 2) > buffer.length)
				buffer = resizeByteArray(buffer, (buffer.length*2));
			
			buffer[currentOffset++] = ":".getBytes()[0];
			buffer[currentOffset++] = (m.isFile()) ? (byte) 0x00 : (byte) 0x01;
			buffer = insertStringByteArray(m.path(), buffer, currentOffset);
			currentOffset += m.path().length();
		}
		
		if( (currentOffset + 2) > buffer.length)
			buffer = resizeByteArray(buffer, (buffer.length+2));
		
		buffer[currentOffset++] = ":".getBytes()[0];
		buffer[currentOffset++] = ":".getBytes()[0];
		
		byte[] rarr = resizeByteArray(buffer, currentOffset);
		
		return rarr;
	}
}
