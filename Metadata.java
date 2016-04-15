import java.util.ArrayList;

public class Metadata {
	private final boolean IS_FOLDER;
	private final String NAME, PATH;
	
	public Metadata(boolean folder, String name, String path){
		NAME = name;
		PATH = path;
		IS_FOLDER = folder;
	}
	
	public String name(){ return NAME.trim(); }
	
	public String path(){ return PATH; }
	
	public boolean isFile(){ return !IS_FOLDER; }
	

	
}
