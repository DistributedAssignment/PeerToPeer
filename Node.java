
import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.Random;
import java.util.concurrent.*;

public class Node {	
	//The data
	static int[] account_list = new int[2048] ;
	//To make it easier to send data to New:Nodes
	static int[] account_index = new int[2048];
	
	//The scoket and packet for sending and receiving
	static DatagramSocket socket_s = null;
	static DatagramSocket socket_r = null;
	static DatagramSocket socket_m = null;
	
 	//The list of nodes on the network
 	static InetAddress[] IP_list = new InetAddress[2048];
 	static String[] ip_list = new String[2048];
 	static int[] port_list = new int[2048];
 	
 	//The information about the node
 	static int port = 1;
 	static String ip_str = getLocalAddress();
 	static InetAddress ip;
 	static int data = 0;
	//This is the index of a changed account
	static int change_index = -1;
	//For out putting information about what the node is doing in the background
 	static int name;
 	/***WORKOUT GIT HUB ISSUE LATER***/
 	public Node() {
 		
 	}
 	
 	public void create() {
		Receiver r = new Receiver();
		Client c = new Client();
		//c.setDaemon(true);
		c.start();
		r.start();
		try{
		c.join();
		r.join();} catch (Exception e) {}
 	}
 	
 	public static void main(String args[]) {
 	 	try {ip = InetAddress.getByName(ip_str);}
 	 	catch (Exception e) {e.printStackTrace();}
	    try {
		    Random ran = new Random();
	 		name = ran.nextInt(100);
	 		System.out.println(name);

	    } catch (Exception e) {e.printStackTrace();}
 		
	    System.err.println("Start");
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
	    	ip = InetAddress.getByName(ip_str);
	    } catch (UnknownHostException e) {e.printStackTrace();}
	
	    boolean setup= false;
		while (setup == false) {
	    	try {
	    		setup = true;
	    		socket_s = new DatagramSocket(port,ip);
			} catch (SocketException e) {
				setup = false;
				port +=1;
			}
	    }
		
	    setup= false;
		while (setup == false) {
	    	try {
	    		setup = true;
	    		socket_m = new DatagramSocket(port,ip);
			} catch (SocketException e) {
				setup = false;
				port +=1;
			}
	    }
		
		
	    setup= false;
		while (setup == false) {
	    	try {
	    		setup = true;
	    		socket_r = new DatagramSocket(port,ip);
			} catch (SocketException e) {
				setup = false;
				port +=1;
			}
	    }
		
		System.out.println(port);
		System.err.println("Initalize");
		
		//The account and port data is read from the repository
		String everything = null;
		int com = -1;
		try {
		String[] commands = {"C:\\Windows\\System32\\cmd.exe", "/c", 
		"I:\\git\\PeerToPeer\\import.bat"};
		File workDir = new File( "I:\\git\\PeerToPeer\\");
		Process process = Runtime.getRuntime().exec( commands, null, workDir);
		System.err.println("Import");
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
				IP_list[i] = null;
				ip_list[i] = null;
				port_list[i] = -1;
			}
			else {
				com = i;
			System.out.println(IP_data[i]);
			try {
				ip_list[i] = IP_data[i];
				IP_list[i] = InetAddress.getByName(IP_data[i]);
				port_list[i] = Integer.parseInt(port_data[i]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			}
		}
		System.err.println("Processed data");
		
			/**HERE THE NODE CONNECTS TO THE NETWORK**/
			//If there are nodes already on the network then the node needs to join it if not it needs to update the repository
			if (com != -1) {
				System.err.println("Joining Network");
				//Adds it self to the node lists
				
				for (int i = 0; i<port_list.length;i++) {
					if (ip_list[i]==null) {
						IP_list[i] = ip;
						ip_list[i] = ip_str;
						port_list[i] = port;
						break;
						
					}
				}	
					
		/*Now it needs to be added to the network it does by contacting the node in com
		 * this will be the node responsible for updating the git repository when it fails and this will be noted in the node
		 * [IF I GOT THIS FAR]
		 * The receiving node creates a listener for this node and when it fails will remove the node from the list update the repository
		 * and notify all other nodes
		 */
		DatagramSocket socket_temp = null;
		DatagramPacket packet = null;
		byte[] up_data = new byte[65536];
			
		int temp_port = 1;	
	    setup= false;
		while (setup == false) {
	    	try {
	    		setup = true;
	    		socket_temp = new DatagramSocket(temp_port,ip);
			} catch (SocketException e) {
				setup = false;
				temp_port +=1;
			}
	    }
		
		try { 
			up_data = ("New:Node:Initial "+port+" "+ip).getBytes();
			packet = new DatagramPacket(up_data, up_data.length,IP_list[com],port_list[com]);	
			socket_temp.send(packet);
			packet = null;
		} catch (Exception e) {e.printStackTrace();}
		
		packet = null;
		byte[] re_data = new byte[65536];
		packet = new DatagramPacket(re_data, re_data.length);
		try { socket_temp.receive(packet);
		} catch (Exception e) {e.printStackTrace();}
		socket_temp.close();
		packet = null;
		System.err.println("Account data received");
		//Now this node should tell all other nodes about this node and will then start to update the other nodes
		//It should also send the account list to this node
		
		//The account data is constructeds
		String temp = new String(re_data);
		data = temp.split(" ");
		String[] a;
		for (int i = 0; i<data.length; i++) {
			a= data[i].split(",");
			int b = Integer.parseInt(a[0].trim());
			int in = Integer.parseInt(a[1].trim());
			account_list[in] = b;
			account_index[in] = in;
		}
		System.err.println("Account data constructed");
		/***THE NODE IS NOW ON THE NETWORK***/
			}
			 else {
				System.err.println("Creating Network");
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
	        	if (ip_list[j]==null) {myWriter.write("NULL ");  	
	        	} else {myWriter.write(ip_list[j]+" "); }
	        }
	        myWriter.close();
			} catch (Exception e) {e.printStackTrace();}
			System.err.println("Data Created");
			//Commits this to the git repository
			try {
				String[] commands = {"C:\\Windows\\System32\\cmd.exe", "/c", 
				"I:\\git\\PeerToPeer\\import.bat"};
				File workDir = new File( "I:\\git\\PeerToPeer\\");
				Process process = Runtime.getRuntime().exec( commands, null, workDir);
			} catch (Exception e) {e.printStackTrace();}
			System.err.println("Data Commited");
			System.err.println("Network Created");
			/***THE NETWORK NOW EXISTS***/
			}
			
			Node node = new Node();
			node.create();
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
		int[] update;
		public Updater(int[] u) {
			this.update = new int [2];
			update[0] = u[0];
			update[1] = u[1];
		}
		
		public void run() {
			byte[] data = new byte[65536];
			try {
				System.err.println("U: Updating");
				String temp_data = "Update "+update[0]+" "+update[1];
				data = temp_data.getBytes();
				for (int i =0;i<IP_list.length;i++) {
					if (port_list[i] != -1) {
						DatagramPacket packet = new DatagramPacket(data, data.length,IP_list[i],port_list[i]);
						socket_s.send(packet);
						packet = null;
					}
				}
				System.err.println("U: Updated");
			} catch (Exception e) {e.printStackTrace();}
		}
	}
	
	private class Messenger extends Thread{
		String[] message;
		public Messenger(String[] ms) {
			this.message = new String[ms.length];
			for (int i = 0; i<ms.length; i++) {
				message[i] = ms[i];
			}
		}
		
		public void run() {
			//For sending data too other nodes
			byte[] data_node = new byte[65536];
			try {
				//Gets the message and acts accordingly
				System.err.println("M: Message received "+String.join(",",message));
				if (message[0].trim().equals("Update")) {
					System.err.println("M: "+message[0].trim());
		 	 		account_list[Integer.parseInt(message[2])] = Integer.parseInt(message[1]);
		 	 		account_index[Integer.parseInt(message[2])] = Integer.parseInt(message[2]);
				} else if (message[0].trim().equals("New:Node")) {
					System.err.println("M: "+message[0].trim());
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
				} else if (message[0].trim().equals("New:Node:Initial")) {
						System.err.println("M: "+message[0].trim());
						String temp = "New:Node "+message[1]+" "+message[2];
						data_node = temp.getBytes();
						for (int i =0;i<IP_list.length;i++) {
							if (port_list[i] != -1) {
								DatagramPacket packet = new DatagramPacket(data_node, data_node.length,IP_list[i],port_list[i]);
								socket_m.send(packet);
								packet = null;
							}
						}
						
						//Updates its node list
						int k = 0;
			 			for (int i = 0; i<ip_list.length; i++) {
			 				if (ip_list[i]==null) {
			 					port_list[i] = Integer.parseInt(message[1]);
			 					ip_list[i] = message[2];	
			 					try {IP_list[i] = InetAddress.getByName(message[2]);
			 					} catch (Exception e) {}
			 					k = i;
			 				}
			 			}
			 			
					String account_dat = "";
				   for (int i = 0; i<2048; i++) {
				        if (account_index[i] != -1) {
				        	account_dat = account_dat + account_list[i] +","+account_index[i] + " ";
				        }
				     }
					DatagramPacket packet = new DatagramPacket(data_node, data_node.length,IP_list[k],port_list[k]);
					socket_m.send(packet);
					packet = null;
					
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
						String[] commands = {"C:\\Windows\\System32\\cmd.exe", "/c", 
						"I:\\git\\PeerToPeer\\import.bat"};
						File workDir = new File( "I:\\git\\PeerToPeer\\");
						Process process = Runtime.getRuntime().exec( commands, null, workDir);
					} catch (Exception e) {e.printStackTrace();}
					
				} 
				System.err.println("M complete");
			} catch (Exception e) {e.printStackTrace();}
		}
	}
	
	private class Receiver extends Thread{
		public Receiver() {
			
		}
		
		public void run() {
			byte[] receive = new byte[65536];
			while (true) { 
				receive = new byte[65536];
				DatagramPacket packet = new DatagramPacket(receive, receive.length);
				try {
					socket_r.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.err.println("R: Received");
				String[] message;	
				String temp = new String(receive);
				System.err.println("R: "+temp);
				System.err.println("R: Sent to messenger");
				message = temp.split(" ");
				System.err.println("R: Sent to messenger");
				(new Messenger(message)).start();
				System.err.println("R: Sent to messenger");
				packet =null;
			}
		}
	}
	
 	//What the client sees
 	private class Client extends Thread{
 		//Request list same as the server except for create as this is for managing a specified account, also no disconnect for similar reasons
 		private static final String[] REQUEST_LIST = {"retreive","withdraw","deposit","close","exit"};
 		private static final String[] MENU_LIST = {"create","manage","disconnect"};
 		private static Scanner myObj = new Scanner(System.in);

 		public Client() {
 		}
 		
 		public void run() {	
 			
 			System.err.println("C: Started");
 			
 			/**NOW THE CLIENT CAN INTERACT WITH THE LOCAL DATA**/
 			while (true) {
 				System.err.println("C: Creation finished");
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
 		    		synchronized(account_list) { u[0] = account_list[change_index];}
 		    		synchronized(account_index) { u[1] = account_index[change_index];}
 		    		(new Updater(u)).start();
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
	    		(new Updater(u)).start();
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
 	 		    			overdraft_str = inputs();
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