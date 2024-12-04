
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Scanner;

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
		
		Updater u = new Updater();
		Receiver r = new Receiver();
		Client c = new Client();
		c.setDaemon(true);
		c.start();
		u.start();
		r.start();
		try{
		c.join();
		u.join();
		r.join();} catch (Exception e) {}
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
	
	private class Messager  extends Thread{
		
		public Messager() {
			
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
					if (message[0].trim().equals("New Node First")) {
						data_node = ("New Node "+message[1]+" "+message[2]).getBytes();
						for (int i =0;i<IP_list.length;i++) {
							if (port_list[i] != -1) {
								DatagramPacket packet = new DatagramPacket(data_node, data_node.length,IP_list[i],port_list[i]);
								socket_s.send(packet);
								packet = null;
							}
						}
					}
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
 		public Client() {
 		}
 		
 		public void run() {		
 			//ADD CONNECTION STUFF HERE
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
