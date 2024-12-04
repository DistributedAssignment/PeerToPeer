
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Scanner;
import java.io.File; 
import java.io.FileWriter;
import java.io.BufferedReader;
import java.lang.ProcessBuilder;
import java.io.FileReader;

public class Node implements Runnable{	
	//The data
	static int[] account_list ;
	//To make it easier to send data to new nodes
	static int[] account_index;
	//This is the index of a changed account
	static int change_index;
	//The scoket and packet for sending and receiving
	static DatagramSocket socket_s;
	static DatagramSocket socket_r;
	//For sending data too other nodes
	static byte[] data;
	//For sending data too other nodes
	static byte[] data_node;
	//For receiving data from other nodes
	static byte[] receive;
	//For out putting information about what the node is doing in the background
 	static PrintStream psB;
 	
 	//The list of nodes on the network
 	static InetAddress[] IP_list;
 	static String[] ip_list;
 	static int[] port_list;
 	
 	//Gets updates information
 	static Queue<int[]> updates;
 	
 	//Gets messages
 	static Queue<String[]> messages;

 	//The information about the node
 	static int port = 1;
 	static String ip_str = getLocalAddress();
 	static InetAddress ip;
 	public Node() {
 		this.account_list = new int[2048];		
 		this.account_list = new int[2048];
 		this.account_index = new int[2048];
 		this.change_index=-1;
 		this.socket_s = null;
 		this.socket_r = null;
 		this.data = new byte[65536];
 		this.receive = new byte[65536];
 	 	this.IP_list = new InetAddress[2048];
 	 	this.ip_list = new String[2048];
 	 	this.port_list = new int[2048];
 	 	this.updates = new LinkedList<int[]>();
 	 	//this.inputs= new LinkedList<String>();
 	 	this.port = 1;
 	 	this.ip_str = getLocalAddress();
 	 	try {this.ip = InetAddress.getByName(ip_str);}
 	 	catch (Exception e) {e.printStackTrace();}
 	}
 	
 	public void run() {
 		//HERE THE NODE IS CREATED
		//-1 is used as an indicator of an empty space
		for (int i = 0; i<account_list.length; i++) {
			account_index[i] = -1;
			port_list[i] = -1;
			//So that no one ends up with a weird starting balance
			account_list[i] = 0;
		}

	    //Sets up the nodes socket
	    try {
		    psB = new PrintStream("Node Data.log");
	    	ip = InetAddress.getByName(ip_str);
	    } catch (UnknownHostException | FileNotFoundException e) {e.printStackTrace();}
	
	    boolean setup= false;
		while (setup == false) {
	    	try {
	    		setup = true;
	    		socket_s = new DatagramSocket(port);
			} catch (SocketException e) {
				setup = false;
				port +=1;
			}
	    }
		
	    setup= false;
		while (setup == false) {
	    	try {
	    		setup = true;
	    		socket_r = new DatagramSocket(port);
			} catch (SocketException e) {
				setup = false;
				port +=1;
			}
	    }
		
		//The account and port data is read from the repository
		String everything = null;
		int com = -1;
		try {
		ArrayList<String> command = new ArrayList<String>();
		command.add(System.getProperty("user.dir")+File.separator+"Import.bat");
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(new File("I:\\git\\PeerToPeer"));
		Process p = pb.start();
		//Reads data
		BufferedReader br = new BufferedReader(new FileReader("Data.txt"));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();

		while (line != null) {
		sb.append(line);
		sb.append(System.lineSeparator());
		line = br.readLine();	
		}
		everything = sb.toString();
		} catch (Exception e) {e.printStackTrace();}
		
		//Processes the data
		String[] data = everything.split("\n");
		//Gets the port, IP and accout list
		String[] port_data = data[0].split(" ");
		String[] IP_data = data[1].split(" ");
		
		//processes the data in the repository
		for (int i = 0; i<port_list.length;i++) {
			if (IP_data[i].trim().equals("NULL")) {
				com = i;
				IP_list[i] = null;
				ip_list[i] = null;
				port_list[i] = -1;
			}
			else {
			try {
				ip_list[i] = IP_data[i];
				IP_list[i] = InetAddress.getByName(IP_data[i]);
				port_list[i] = Integer.parseInt(port_data[i]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			}
		}
		
		Updater u = new Updater();
		Receiver r = new Receiver();
		Client c = new Client(com);
		Messenger m = new Messenger();
		//c.setDaemon(true);
		c.start();
		u.start();
		r.start();
		m.start();
		try{
		c.join();
		u.join();
		r.join();
		m.join();} catch (Exception e) {}
		System.out.println("Hello");
	}
 	
 	
 	public static void main(String args[]) {
	    try {
		    psB = new PrintStream("Node Data.log");
		   System.setErr(psB);
	    } catch (FileNotFoundException e) {e.printStackTrace();}
 		Node n = new Node();
 		n.run();
 	}

	//For retrieving the IP
    private static String getLocalAddress() {
    try (DatagramSocket skt = new DatagramSocket()) {
        // Use default gateway and arbitrary port
        skt.connect(InetAddress.getByName("192.168.1.1"), 12345);
        return skt.getLocalAddress().getHostAddress();
    } catch (Exception e) {
        return null;
    }
}
			
	private class Updater extends Thread{
		
		public Updater() {
			
		}
		
		public void run() {
			while (true) {
			try {

				//Gets the update from the updates
				int[] update = updates.remove();
				String temp_data = "Update "+update[0]+" "+update[1];
				data = temp_data.getBytes();
				for (int i =0;i<IP_list.length;i++) {
					if (port_list[i] != -1) {
						DatagramPacket packet = new DatagramPacket(data, data.length,IP_list[i],port_list[i]);
						socket_s.send(packet);
						packet = null;
					}
				}
			} catch (Exception e) {}
			}
		}
	}
	
	private class Messenger extends Thread{
		
		public Messenger() {
			
		}
		
		public void run() {
			while (true) {
			try {

				//Gets the message and acts accordingly
				String[] message = messages.remove();
				if (message[0].trim().equals("Update")) {
		 	 		account_list[Integer.parseInt(message[2])] = Integer.parseInt(message[1]);
		 	 		account_index[Integer.parseInt(message[2])] = Integer.parseInt(message[2]);
				} else if (message[0].trim().equals("New Node ")) {
					//Updates its node list
		 			for (int i = 0; i<ip_list.length; i++) {
		 				if (ip_list[i]==null) {
		 					port_list[i] = Integer.parseInt(message[1]);
		 					ip_list[i] = message[2];	
		 					try {IP_list[i] = InetAddress.getByName(message[2]);
		 					} catch (Exception e) {}
		 				}
		 			}
					
					//If this is the node with initial contact, all nodes are updated by it
				} else if (message[0].trim().equals("New Node First")) {
						data_node = ("New Node "+message[1]+" "+message[2]).getBytes();
						for (int i =0;i<IP_list.length;i++) {
							if (port_list[i] != -1) {
								DatagramPacket packet = new DatagramPacket(data_node, data_node.length,IP_list[i],port_list[i]);
								socket_s.send(packet);
								packet = null;
							}
						}
					
					
					//Reconstructs data
					try {
			        FileWriter myWriter = new FileWriter("Data.txt");
			        for (int j = 0; j<2048; j++) {
			        	myWriter.write(port_list[j]+" ");
			        }
			        myWriter.write("\n");
			        for (int j = 0; j<2048; j++) {
			        	if (ip_list.equals(null)) {myWriter.write("NULL ");  	
			        	} else {myWriter.write(ip_list[j]+" ");}
			        }
			        myWriter.close();
					} catch (Exception e) {e.printStackTrace();}
					
					//Commits this to the git repository
					try {
					ArrayList<String> command = new ArrayList<String>();
					command.add(System.getProperty("user.dir")+File.separator+"Commit.bat");
					ProcessBuilder pb = new ProcessBuilder(command);
					pb.directory(new File("I:\\git\\PeerToPeer"));
					Process p = pb.start();
					} catch (Exception e) {e.printStackTrace();}
					
				} 
			} catch (Exception e) {}
			}
		}
	}
	
	private class Receiver extends Thread{
		
		public Receiver() {
			
		}
		
		public void run() {
			while (true) { 
				DatagramPacket packet = new DatagramPacket(receive, receive.length);
				try {
					socket_r.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				String[] message;	
				String temp = new String(receive);
				message = temp.split(" ");
				messages.add(message);
			}
		}
	}
	
 	//What the client sees
 	private class Client extends Thread{
 		//Request list same as the server except for create as this is for managing a specified account, also no disconnect for similar reasons
 		private static final String[] REQUEST_LIST = {"retreive","withdraw","deposit","close","exit"};
 		private static final String[] MENU_LIST = {"create","manage","disconnect"};
 		private static Scanner myObj = new Scanner(System.in);
 		int com;
 		public Client(int c) {
 			this.com = c;
 		}
 		
 		public void run() {		
 			/**HERE THE NODE CONNECTS TO THE NETWORK**/
 			//If there are nodes already on the network then the node needs to join it if not it needs to update the repository
 			int com = -1;
 			if (com != -1) {
 				//Adds it self to the node lists
 				for (int i = 0; i<port_list.length;i++) {
 					if (ip_list[i].trim().equals("NULL")) {
 						IP_list[i] = ip;
 						ip_list[i] = ip_str;
 						port_list[i] = port;
 					}
 				}	
 					
 					/*Now it needs to be added to the network it does by contacting the node in com
 					 * this will be the node responsible for updating the git repository when it fails and this will be noted in the node
 					 * [IF I GOT THIS FAR]
 					 * The receiving node creates a listener for this node and when it fails will remove the node from the list update the repository
 					 * and notify all other nodes
 					 */
 					byte[] up_data = ("New Node Initial "+port+" "+ip).getBytes();
 					DatagramPacket packet = new DatagramPacket(up_data, up_data.length,IP_list[com],port_list[com]);
 					try { socket_s.send(packet);
 					} catch (Exception e) {e.printStackTrace();}
 					
 					packet = null;
 					
 					//Now this node should tell all other nodes about this node and will then start to update the other nodes
 					//It should also send the account list to this node
 					
 					//The account data is constructeds
 					byte[] re_data = new byte[65536];
 					packet = new DatagramPacket(re_data,re_data.length);
 					String temp = new String(re_data);
 					String[] data = temp.split(" ");
 					String[] a;
 					for (int i = 0; i<data.length; i++) {
 						a= data[i].split(",");
 						int b = Integer.parseInt(a[0].trim());
 						int in = Integer.parseInt(a[1].trim());
 						account_list[in] = b;
 						account_index[in] = in;
 					}
 				/***THE NODE IS NOW ON THE NETWORK***/
 			}
 			 else {
 				//Adds itself to the list
 				ip_list[0] = ip_str;
 				IP_list[0]= ip;
 				port_list[0] = port;
 			
 			try {
 	        FileWriter myWriter = new FileWriter("Data.txt");
 	        for (int j = 0; j<2048; j++) {
 	        	myWriter.write(port_list[j]+" ");
 	        }
 	        myWriter.write("\n");
 	        for (int j = 0; j<2048; j++) {
 	        	if (ip_list[j].equals(null)) {myWriter.write("NULL ");  	
 	        	} else {myWriter.write(ip_list[j]+" "); }
 	        }
 	        myWriter.close();
 			} catch (Exception e) {e.printStackTrace();}
 			
 			//Commits this to the git repository
 			try {
 			ArrayList<String> command = new ArrayList<String>();
 			command.add(System.getProperty("user.dir")+File.separator+"Commit.bat");
 			ProcessBuilder pb = new ProcessBuilder(command);
 			pb.directory(new File("I:\\git\\PeerToPeer"));
 			Process p = pb.start();
 			System.out.println("Commited");
 			} catch (Exception e) {e.printStackTrace();}
 			/***THE NETWORK NOW EXISTS***/
 			}
 			
 			/**NOW THE CLIENT CAN INTERACT WITH THE LOCAL DATA**/
 			while (true) {
 				String in = "";
 				int number;
 				System.out.println("Please press: ");
 		    	for (int i=0; i<MENU_LIST.length; i++) {
 		    		int n = i+1;
 		    		System.out.println(n+": "+MENU_LIST[i]);
 		    		
 		    	}
 		    	System.out.print("Input: ");
 		    	try { 
 		    	boolean input =false;
 		    	in = myObj.nextLine();
 		    	in.trim();
 		    	number = Integer.parseInt(in);
 				}
 				catch(Exception NumberFormatException) {
 				  number = 0;
 				}
 		    	//The account number
 				String account = "";
 				switch(number) {
 				  case 1:
 				    createAccount();
 				    break;
 				  case 2:
 					try {
 			    	System.out.print("Account number: ");
 	 		    	boolean input =false;
 	 		    	while (!input) {
 	 		    		try {
 	 		    			account = myObj.nextLine();
 	 		    			input = true;
 	 		    		} catch (Exception e) {
 	 		    			input= false;
 	 		    		}
 	 		    	}
 			    	account.trim();  
 					} catch (Exception e) {
 						account = "0";
 					}
 				    if (retrieveData(account)) {
 				    	while (accountManagement(account));
 				    }
 				   break;
 				  case 3:
 					    break;
 				  default:
 		  		  System.out.println("Invalid request");
 		  		  break;
 				}
 				number = 0;
 		    	if (change_index!=-1) {
 		    		System.out.println("Worked");
 		    		int[] u = new int[2];
 		    		u[0] = account_list[change_index];
 		    		u[1] = account_index[change_index];
 		    		updates.add(u);
 		    		change_index = -1;
 		    	}
 				}
 		}
 		
 		private synchronized  boolean accountManagement(String account) {
 	    	Scanner myObj = new Scanner(System.in);
 	    	int number = 0;
 	    	String choice = "";
 	    	System.out.println("Account "+account);
 	    	System.out.println("Please press: ");
 	    	for (int i=0; i<REQUEST_LIST.length; i++) {
 	    		int n = i+1;
 	    		System.out.println(n+": "+REQUEST_LIST[i]);
 	    		
 	    	}
 			try {
 		    	System.out.print("Input: ");
 		    	boolean input =false;
 		    	while (!input) {
 		    		try {
 		    			choice = myObj.nextLine();
 		    			input = true;
 		    		} catch (Exception e) {
 		    			input= false;
 		    		}
 		    	}
 		    	choice.trim();
 		    	number = Integer.parseInt(choice);
 				}
 				catch(Exception NumberFormatException) {
 				  number = 0;
 				}
 	    	boolean b;
 	    	switch(number) {
 	    	  case 1:
 	    		b = retrieveData(account);
 	    		return b;
 	    	  case 2:
 	    		 withdraw(account);
 	    		b = true;
 	    	  case 3:
 	    		deposit(account);
 	    		b = true;
 	    	  case 4:
 	      		closeData(account);
 	      		b = false;
 	    	  case 5:
 	    		  b = false;
 	    	  default:
 	    		  System.out.println("Invalid request");
 	    		  b = false;
 	    	}
 	    	
 	    	if (change_index!=-1) {
	    		int[] u = new int[2];
	    		synchronized(account_list) {u[0] = account_list[change_index];}
	    		synchronized(account_index) {u[1] = account_index[change_index];}
	    		synchronized(updates) {updates.add(u);}
 	    		change_index = -1;
 	    	}
 	    	
 	    	return b;
 	    }
 		
 		private synchronized boolean retrieveData(String account)  {	
 			try {if (account_index[Integer.parseInt(account)]!= -1) {
 			System.out.println("--------------------------");

 				System.out.println("Account number: " + account_index[Integer.parseInt(account)]);
 				System.out.println("Account balance: " + account_list[Integer.parseInt(account)]);
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 				return true;
 			} else {
 				System.out.println("No account found");
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 				return false;
 			}
 			} catch (Exception e) {
 				System.out.println("Invalid input");
 				change_index = -1;
 				return false;
 			}
 	    }
 	        
 	    private synchronized  void withdraw(String account)  {
 		 	boolean withdraw = false;
 		 	int money = 0;
 		 	String money_str = null;
 		 	while (!(withdraw)) {
 		 	
 		 	//Here the user is asked to input there desired withdraw
 			try {
 	    	System.out.print("How much would you like to withdraw: ");
		    	boolean input =false;
		    	while (!input) {
		    		try {
		    			money_str = myObj.nextLine();
		    			input = true;
		    		} catch (Exception e) {
		    			input= false;
		    		}
		    	}
 	    	money_str.trim();
 	    	money= Integer.parseInt(account);
 			}
 			catch(Exception NumberFormatException) {
 			 money = -1;
 			 money_str = "-1";
 			}
 			
 			int balance = -1000;
 			if (account_index[Integer.parseInt(account)]==-1) {
 				try { balance = -2;
 				} catch (Exception e) {
 					balance = -1;
 				}
 		 	} else {
 		 		if (0<=account_list[Integer.parseInt(account)] - money) {
 		 		account_list[Integer.parseInt(account)] -= money;} else {
 		 			balance = -3;
 		 		}
 		 	}

 			
 			System.out.println("--------------------------");
 			if (balance == -3) {
 				System.out.println("Insuffcient funds");
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 				
 			} else if (balance == -1) {
 				System.out.println("Inavlid input");
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 			}else if (balance == -2) {
 				System.out.println("Account no longer exists");
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 			}else {
 				System.out.println("Account balance: " + balance);
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = Integer.parseInt(account);
 			}
 		 	}
 	 }
 	 
 	    private synchronized  void deposit(String account)  {
 		 	boolean deposit =  false;
 		 	int money = 0;
 		 	String money_str = null;
 		 	while (!(deposit)) {
 		 	
 		 	//Here the user is asked to input there desired withdraw
 			try {
 	    	System.out.print("How much would you like to deposit: ");
		    	boolean input =false;
		    	while (!input) {
		    		try {
		    			money_str = myObj.nextLine();
		    			input = true;
		    		} catch (Exception e) {
		    			input= false;
		    		}
		    	}
 	    	money_str.trim();
 	    	money= Integer.parseInt(account);
 			}
 			catch(Exception NumberFormatException) {
 			 money = -1;
 			 money_str = "-1";
 			}
 			
 			
 			int balance = -1000;
 			if (account_index[Integer.parseInt(account)]==-1) {
 				try { balance = -2;
 				} catch (Exception e) {
 					balance = -1;
 				}
 		 	} else {account_list[Integer.parseInt(account)] += money;}
 			
 			System.out.println("");
 			System.out.println("--------------------------");
 			if (balance == -1) {
 				System.out.println("Invalid Input");
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 			}else if (balance == -2) {
 				System.out.println("Account does not exist");
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 				
 			} else {
 				System.out.println("Account balanc: " + balance);
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = Integer.parseInt(account);
 			}
 		 	}
 	 }

 	    private synchronized void createAccount()  {
 	        Scanner myObj = new Scanner(System.in);
 			boolean create = false;
 			int overdraft = 0;
 			/*while (!create) {
 				String overdraft_str = null;
 		    	System.out.print("Please input your desired overdraft limit (0-1500): ");
 				try {
 	 		    	boolean input =false;
 	 		    	while (!input) {
 	 		    		try {
 	 		    			overdraft_str = inputs.remove();
 	 		    			input = true;
 	 		    		} catch (Exception e) {
 	 		    			input= false;
 	 		    		}
 	 		    	}
 			    	overdraft_str.trim();
 			    	overdraft = Integer.parseInt(overdraft_str);
 		
 					if (overdraft >= 0 && overdraft <= 1500) {
 						create = true;
 					}
 					}
 					catch(Exception NumberFormatException) {
 					System.out.println("Not a number"); 
 					}
 			}*/
 			
 			//Finds a missing spot
 			int ind = -1;
 			for (int i = 0; i<account_index.length; i++) {
 				if (account_index[i]==-1) {
 					ind = i;
 					break;
 				}
 			}
 			//1 indicates it is being used
 			change_index = ind;
 	 		account_index[ind] = 1;
 			System.out.println("Account number: " + ind);
 	    }
 	 
 	    private synchronized boolean closeData(String account)  {
 			try {if (account_index[Integer.parseInt(account)]!= -1) {
 			System.out.println("--------------------------");
 				account_index[Integer.parseInt(account)] = -1;
 				change_index = -1;
 				return true;
 			} else {
 				System.out.println("No account found");
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 				return false;
 			}
 			} catch (Exception e) {
 				System.out.println("Invalid input");
 				change_index = -1;
 				return false;
 			}
 	    }
 	    
 }
	
}
