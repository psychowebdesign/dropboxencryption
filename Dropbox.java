/**
 * Dropbox interface
 */
import com.dropbox.core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Dropbox {
	
	final String CONFIG_FILE = "dropbox.config";
	Map<String, String> CONFIG;
	DbxClient CLIENT;
	DbxRequestConfig REQUEST_CONFIG;
	private String CUR_DIR, TEMP_DIR;
	
	public Dropbox(){
		setupConfig(); //this must be executed first always!
		CLIENT = new DbxClient(REQUEST_CONFIG, CONFIG.get("ACCESS_TOKEN").trim());
		CUR_DIR = "/";
	}
	
	/**
	 * print current CUR_DIR
	 * @return
	 */
	public String pwd(){ return CUR_DIR; }
	
	/**
	 * Change current CUR_DIR
	 * @param dir
	 * @return
	 */
	public boolean cd(String dir){
		for(Metadata mdata : ls()){
			if(mdata.name().equals(dir)){
				CUR_DIR += "/" + dir;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * List directory CUR_DIR on dropbox
	 * @return
	 */
	public Metadata[] ls(){
		return ls(CUR_DIR);
	}
	
	/**
	 * Retrive a file from dropbox
	 * @param retrievePath String path to dropbox file
	 * @param storePath String path to store file
	 * @return
	 */
	public boolean download(String retrievePath, String storePath){
		FileOutputStream outputStream;
		try{
			outputStream = new FileOutputStream(storePath.trim());
			DbxEntry.File downloadFile = CLIENT.getFile(retrievePath.trim(), null, outputStream);
			outputStream.close();
		} catch (DbxException | IOException e) { e.printStackTrace(); }
		
		return true;
	}
	
	/**
	 * Gets filename from a path
	 * @param path String path to file
	 * @return String name of file taken from path
	 */
	private String getFileName(String path){
		String[] parts = path.split("/");
		return parts[parts.length-1];
	}
	
	/**
	 * Creates a Metadata[] listing of specified directory
	 * @param dir String directory to list
	 * @return
	 */
	public Metadata[] ls(String dir){
		ArrayList<Metadata> mData = new ArrayList<Metadata>();
		DbxEntry.WithChildren listing;
		try {
			//System.out.println("calling - " + CUR_DIR);
			listing = CLIENT.getMetadataWithChildren(dir);
			if(listing != null){
				for (DbxEntry child : listing.children) {
					mData.add(new Metadata(child.isFolder(), child.name, child.path));
				}
			} else { return null; }

		} catch (DbxException e) { e.printStackTrace();	}
		
		Metadata[] metaData = new Metadata[mData.size()];
		return mData.toArray(metaData);
	}
	
	/**
	 * Upload a file to dropbox
	 * @param path String path to file to be uploaded
	 * @param destination String path to dropbox location to upload file
	 * @return
	 */
	public boolean upload(String path, String destination){
		File inputFile = new File(path.trim());
		System.out.println("destination hey " + destination);
		if(!inputFile.exists()){
			System.out.println("File doesn't exist! " + path);
			return false;
		}
		
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(inputFile);
			System.out.println("");
		    DbxEntry.File uploadedFile = CLIENT.uploadFile(destination,
		        DbxWriteMode.add(), inputFile.length(), inputStream);
		    System.out.println("Uploaded: " + uploadedFile.toString());
		    inputStream.close();
		} catch (IOException | DbxException e) { e.printStackTrace(); }
		
		return true;
	}
	
	/**
	 * Sets up dropbox connection. If not present, creates and saves an authorisation code.
	 */
	private void setupConfig(){
		REQUEST_CONFIG = new DbxRequestConfig(
		        "JavaTutorial/1.0", Locale.getDefault().toString());
		//check if config file exists, create if not
		File configFile = new File(CONFIG_FILE);
		boolean flushConfig = false;
		
		if(!configFile.exists()){ 
			flushConfig = true;
			try {
				configFile.createNewFile();
			} catch (IOException e) { e.printStackTrace(); }
		}
		
		//get config
		CONFIG = parseConfigFile(CONFIG_FILE);
		
		//check that we have an access token, get one if not
		if(!CONFIG.containsKey("ACCESS_TOKEN")){ 
			flushConfig = true;
			CONFIG.put("ACCESS_TOKEN", getAccessToken());
		}
		
		if(flushConfig){
			writeConfigFile(CONFIG, CONFIG_FILE);
		}
	}
	
	/**
	 * Writes the current config to disk
	 * @param config Map<String, String> config to save to file
	 * @param configFile String path to config file
	 */
	private void writeConfigFile(Map<String, String> config, String configFile){
		if(!config.isEmpty()){
			try {
				BufferedWriter buffWrite = new BufferedWriter(new FileWriter(configFile));
				for (Map.Entry<String, String> entry : config.entrySet()){
				    buffWrite.write(entry.getKey() + ": " + entry.getValue());
				}
				
				buffWrite.close();
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/**
	 * Read config from disk
	 * @param configFile String path to config file on disk
	 * @return
	 */
	private Map<String, String> parseConfigFile(String configFile){
		Map<String, String> config = new HashMap<String, String>();
		try {
			BufferedReader buffRead = new BufferedReader(new FileReader(configFile));
			String line;
			
			while((line = buffRead.readLine()) != null){
				String[] parts = line.split(":");
				if(parts.length != 2){ System.out.println("WTF is this? " + line);}
				config.put(parts[0].trim(), parts[1].trim());
			}
			
		} catch (IOException e) { e.printStackTrace(); }
		return config;
	}
	
	/**
	 * Gets the access token from dropbox. Requires terminal input
	 * @return
	 */
	private String getAccessToken(){
	    final String APP_KEY = "duubmjg5dmqccl7";
	    final String APP_SECRET = "1uccnu0iefw689x";
	
	    DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
	
	    
	    DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(REQUEST_CONFIG, appInfo);
	    
	    String authorizeUrl = webAuth.start();
	    System.out.println("1. Go to: " + authorizeUrl);
	    System.out.println("2. Click \"Allow\" (you might have to log in first)");
	    System.out.println("3. Copy the authorization code.");
	    String code = "";
	    String accessToken = "";
		try {
			code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
			DbxAuthFinish authFinish = webAuth.finish(code);
		    accessToken = authFinish.accessToken;
		} catch (IOException | DbxException e) { e.printStackTrace(); }
		
		return accessToken;
	}
}
 