import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class Explorer extends JFrame implements ActionListener{

	private ImageIcon folder_icon = new ImageIcon("folder.jpg");
	private ImageIcon file_icon = new ImageIcon("file.jpg");
	Object[][] currentData;
	JTable table;
	String[] COLUMN_NAMES = {"", "Name", "Type"};
	Client client;
	JFileChooser fileChooser;
	JButton backButton, openFileButton;
	
	
	Explorer(){
		
		//get port number to connect to, currently hardcoded to localhost for demo
		int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number..."));
		client = new Client("127.0.0.1", port);
		
		//if client.ls() returns null, the client is unauthorised to connect to the server
		Metadata[] initialListing = client.ls();
		if(initialListing != null && initialListing.length > 0){
			//set up GUI
		
			fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			
			openFileButton = new JButton("open");
			openFileButton.setActionCommand("open");
			openFileButton.addActionListener(this);
			backButton = new JButton("back");
			backButton.setActionCommand("back");
			backButton.addActionListener(this);
			
			currentData = transformDropboxMetadata(initialListing);
			JPanel tablePanel = new JPanel(new FlowLayout());		
			//Object[][] transformedData = transfromExplorerData(savedData);
			ExplorerTableModel model = new ExplorerTableModel(currentData, COLUMN_NAMES);
			table = new JTable(model);
			table.setPreferredScrollableViewportSize(new Dimension(500, 100));
			
			table.addMouseListener(new MouseAdapter() {
				  public void mouseClicked(MouseEvent e) {
				    if (e.getClickCount() == 2) {
				      JTable target = (JTable)e.getSource();
				      int row = target.getSelectedRow();
				      int column = target.getSelectedColumn();
				      if(column == 1)
				    	  update(row);
				    }
				  }
			});
			
			table.setRowHeight(25);
			
			tablePanel.add(backButton);
			tablePanel.add(openFileButton);
			tablePanel.add(new JScrollPane(table));
			
			add(tablePanel);
		} else { //client used is unauthorised by the server
			displayUnauthorised();
		}	
	}
	
	/**
	 * Displays an unathorised message to user on GUI
	 */
	private void displayUnauthorised(){
		getContentPane().removeAll();
		JPanel infoPanel = new JPanel(new FlowLayout());

		JLabel label = new JLabel("UNAUTHORISED!");
		infoPanel.add(label);
		getContentPane().add(infoPanel);
		
		getContentPane().repaint();
		getContentPane().revalidate();

		System.out.println("not authorized");
	}
	
	/**
	 * Generates table data from a Metadata[]. Convenience method for GUI
	 * @param mdata
	 * @return
	 */
	private Object[][] transformDropboxMetadata(Metadata[] mdata){
		Object[][] new_data = new Object[mdata.length][3];
		
		for(int i=0; i<mdata.length;i++){
			new_data[i][0] = (mdata[i].isFile()) ? file_icon : folder_icon;
			new_data[i][1] = mdata[i].name();
			new_data[i][2] = (mdata[i].isFile()) ? "File" : "Folder";
		}
		
		return new_data;
	}
	
	/**
	 * Row double-click handler
	 * If row represents a directory, the current working directory will be changed to that
	 * pointed to by the row, and a new listing of that directory will be displayed to the user
	 * If row represents a file, the file will be downloaded and stored in clientTemp directory
	 * @param row
	 */
	private void update(int row){
		if(((String)currentData[row][2]).equals("Folder")){
			String dirName = (String) currentData[row][1];
			client.cd(dirName);
			Metadata[] listing = client.ls();
			
			if(listing == null){
				displayUnauthorised();
				return;
			}
			
			Object[][] new_data = transformDropboxMetadata(listing);
			currentData = new_data;
			ExplorerTableModel model = (ExplorerTableModel) table.getModel();
			model.updateData(new_data);
			model.fireTableDataChanged();
			//ExplorerTableModel model = new ExplorerTableModel(transformedData, COLUMN_NAMES);
			//table.setModel(model);
		} else if(((String)currentData[row][2]).equals("File")){
			String fileName = (String) currentData[row][1];
			if(!client.download(fileName)){
				displayUnauthorised();
			}

		} else { System.out.println("ACCESS ERROR");}


	}
	
	/**
	 * Table model for main GUI table
	 *
	 */
	class ExplorerTableModel extends AbstractTableModel{
		
		private Object[][] data;
		int rows, cols;
		String[] colNames;
		
		ExplorerTableModel(Object[][] new_data, String[] col_names){
			data = new_data;
			rows = new_data.length;
			cols = new_data[0].length;
			colNames = col_names;
			assert(new_data[0].length == col_names.length);
		}
		
		public void updateData(Object[][] new_data){
			data = new_data;
			rows = new_data.length;
		}
		
		
		public boolean isEditable(){
			return false;
		}

		@Override
		public int getColumnCount() {
			return cols;
		}

		@Override
		public int getRowCount() {
			return rows;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return data[rowIndex][columnIndex];
		}
		
		public Class getColumnClass(int column){
			return getValueAt(0, column).getClass();
		}
		
		public String getColumnName(int column) {
			return colNames[column];
		}
		
	}

	/**
	 * Main entry for Explorer
	 * @param args
	 */
	public static void main(String[] args) {
		Explorer exp = new Explorer();
		exp.setVisible(true);
		exp.setSize(600,200);
		exp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//exp.pack();
	}

	/**
	 * Returns file path of gile selected by JFileChooser
	 * @return
	 */
	private String getFileInputPath(){
		int status = fileChooser.showOpenDialog(this);
		if(status == JFileChooser.APPROVE_OPTION){
			File inputFile = fileChooser.getSelectedFile();
			return inputFile.getAbsolutePath();
		}
		
		return "";
	}
	
	/**
	 * Main handler for buttons
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object[][] new_data;
		ExplorerTableModel model;
		switch(arg0.getActionCommand()){
			case "open":
				
				String uploadSrc = getFileInputPath();
				if(uploadSrc.length() > 0){
					if(client.upload(uploadSrc)){
						new_data = transformDropboxMetadata(client.ls());
						currentData = new_data;
						model = (ExplorerTableModel) table.getModel();
						model.updateData(new_data);
						model.fireTableDataChanged();
					} else {
						displayUnauthorised();
					}
				}
					
				break;
			case "back":
				client.back();
				Metadata[] listing = client.ls();
				
				if(listing == null){
					displayUnauthorised();
					return;
				} else {
					new_data = transformDropboxMetadata(listing);
					currentData = new_data;
					model = (ExplorerTableModel) table.getModel();
					model.updateData(new_data);
					model.fireTableDataChanged();
					System.out.println("backed");
				}
				break;
		}
	}
}
