import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ServerGui extends JFrame implements ActionListener{

	JPanel listPanel;
	JLabel label;
	DefaultListModel whitelistModel;
	//whitelist stores IPs of clients allowed to contact server
	JList<String> whitelist;
	JTextField inputText;
	JButton removeButton;
	Server server;
	
	ServerGui(){
		listPanel = new JPanel(new GridBagLayout());

		//set up GUI
		whitelistModel = new DefaultListModel();
		whitelist = new JList<String>(whitelistModel);
		whitelist.addListSelectionListener(new ListSelectionListener()
		{
		  public void valueChanged(ListSelectionEvent ev)
		  {
		    removeButton.setEnabled(true);
		  } 
		});

		whitelist.setVisibleRowCount(10);
		whitelist.setFixedCellHeight(40);
		whitelist.setFixedCellWidth(200);
		
		inputText = new JTextField(30);
		inputText.setToolTipText("Add IP to whitelist");
		inputText.addActionListener(this);
		inputText.setActionCommand("input");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0,0,0,0);
	    gbc.gridx = 0;
	    gbc.gridy = 0;
		listPanel.add(new JScrollPane(whitelist), gbc);
	    gbc.gridx = 0;
	    gbc.gridy = 1;
		listPanel.add(inputText, gbc);
		
		removeButton = new JButton("Remove");
		removeButton.setActionCommand("remove");
		removeButton.addActionListener(this);
		removeButton.setEnabled(false);
		 gbc.gridx = 0;
		 gbc.gridy = 2;
		listPanel.add(removeButton, gbc);

		add(listPanel);
		server = new Server(8001, getWhitelistArray());
		server.start();
	}
	
	/**
	 * Returns array of all IPs allowed to connect to the server
	 * @return
	 */
	public String[] getWhitelistArray(){
		String[] list = new String[whitelistModel.getSize()];
		Enumeration<String> listEnum = whitelistModel.elements();
		int i = 0;
		while(listEnum.hasMoreElements()){
			list[i++] = (String) listEnum.nextElement();
		}
		
		return list;
	}
	
	/**
	 * Main entry point for server
	 * @param args
	 */
	public static void main(String[] args){
		ServerGui sg = new ServerGui();
		sg.setVisible(true);
		sg.setSize(600,500);
		sg.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//exp.pack();
	}

	/**
	 * Main button handler
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		System.out.println("in here");  
		switch(arg0.getActionCommand()){
			case "input":
				System.out.println("input");
				String text = inputText.getText();		
				whitelistModel.addElement(text);
				server.setWhitelist(getWhitelistArray());
				inputText.setText("");
				break;
			case "remove":
				System.out.println("remove");
				whitelistModel.remove(whitelist.getSelectedIndex());
				server.setWhitelist(getWhitelistArray());
				removeButton.setEnabled(false);
		}
		
	}
}
