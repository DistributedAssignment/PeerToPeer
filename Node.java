
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
import java.util.Random;
import java.util.concurrent.*;

public class Node {	
	//The data
	static int[] account_list = new int[2048] ;
	//To make it easier to send data to News
	static int[] account_index = new int[2048];
	
	//The scoket and packet for sending and receiving
	static DatagramSocket socket_s = null;
	static DatagramSocket socket_r = null;
	static DatagramSocket socket_m = null;
	static DatagramSocket socket_c = null;
 	//The list of nodes on the network
 	static InetAddress[] IP_list = new InetAddress[2048];
 	static String[] ip_list = new String[2048];
 	static int[] port_list = new int[2048];
 	static String[] name_list = new String[2048];
 	static int[] index_list = new int[2048];
 	//The information about the node
 	static int port = 1;
 	static String ip_str = getLocalAddress();
 	static InetAddress ip;
 	static int data = 0;
 	static int inds = 0;
	//This is the index of a changed account
	static int change_index = -1;
	//For out putting information about what the node is doing in the background
 	static String name;
 	/***WORKOUT GIT HUB ISSUE LATER***/
 	public Node() {
 		
 	}
 	
 	public void create() {
		Receiver r = new Receiver();
		Client c = new Client();
		c.start();
		r.start();
		try{
		c.join();
		r.join();} catch (Exception e) {}
 	}
 	
 	public static void main(String args[]) {
 		System.out.println("Connecting... ");
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
	    		socket_c = new DatagramSocket(port,ip);
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
		name = "Node#network.."+ip_str+"#port.."+port;
		System.out.println(name);
		
		//The account and port data is read from the repository
		String everything = null;
		int com = -1;
		try {
			ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "Import.bat");
			File dir = new File("I:\\git\\PeerToPeer");
			pb.directory(dir);
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
		String[] port_data = data[0].split(";");
		String[] IP_data = data[1].split(";");
		String[] name_data = data[2].split(";");
		String[] index_data = data[3].split(";");
		//processes the data in the repository
		for (int i = 0; i<port_list.length;i++) {
			if ((IP_data[i].trim()).equals("null")) {
				IP_list[i] = null;
				ip_list[i] = null;
				port_list[i] = -1;
				name_list[i] = null;
				index_list[i] = -1;
			}
			else {
			com = i;
			try {
				name_list[i] = name_data[i].trim();
				ip_list[i] = IP_data[i].trim();
				IP_list[i] = InetAddress.getByName(IP_data[i].trim());
				port_list[i] = Integer.parseInt(port_data[i].trim());
				index_list[i] = Integer.parseInt(index_data[i].trim());
			} catch (Exception e) {
				e.printStackTrace();
			}
			}
		}
		
			/**HERE THE NODE CONNECTS TO THE NETWORK**/
			//If there are nodes already on the network then the node needs to join it if not it needs to update the repository
			if (com != -1) {
				//Adds it self to the node lists
				
				for (int i = 0; i<port_list.length;i++) {
					if (ip_list[i]==null) {
						IP_list[i] = ip;
						ip_list[i] = ip_str;
						port_list[i] = port;
						name_list[i] = name;
						inds = i;
						break;
						
					}
				}
				index_list[inds] = inds;
					
		/*Now it needs to be added to the network it does by contacting the node in com
		 * this will be the node responsible for updating the git repository when it fails and this will be noted in the node
		 * [IF I GOT THIS FAR]
		 * The receiving node creates a listener for this node and when it fails will remove the node from the list update the repository
		 * and notify all other nodes
		 */
		DatagramPacket packet = null;
		byte[] up_data = new byte[65536];
		try {
			up_data = ("NewI;"+port+";"+ip_str+";"+name+";"+inds).getBytes();
			packet = new DatagramPacket(up_data, up_data.length,IP_list[com],port_list[com]);	
			socket_r.send(packet);
			packet = null;
		} catch (Exception e) {e.printStackTrace();}
		
		packet = null;
		byte[] re_data = new byte[65536];
		packet = new DatagramPacket(re_data, re_data.length);
		try { socket_r.receive(packet);
		} catch (Exception e) {e.printStackTrace();}		
		packet = null;
		//Now this node should tell all other nodes about this node and will then start to update the other nodes
		//It should also send the account list to this node
		
		//The account data is constructeds
		String temp = new String(re_data);
		temp = temp.trim();
		if (temp.length()>2) {
			String[] account_arr = temp.split(";");
			for (int i = 0; i<account_arr.length; i++) {
				String[] a= account_arr[i].split(",");
				int b = Integer.parseInt(a[0].trim());
				int in = Integer.parseInt(a[1].trim());
				account_list[in] = b;
				account_index[in] = in;
			}
			
		}
		/***THE NODE IS NOW ON THE NETWORK***/
			}
			 else {
				//Adds itself to the list
				ip_list[0] = ip_str;
				IP_list[0]= ip;
				port_list[0] = port;
				name_list[0] = name;
				index_list[0] = 0;
			
			try {
	        FileWriter myWriter = new FileWriter("Data.txt");
	        for (int j = 0; j<2048; j++) {
	        	myWriter.write(port_list[j]+";");
	        }
	        myWriter.write("\n");
	        for (int j = 0; j<2048; j++) {
	        	if (ip_list[j]==null) {myWriter.write("null;");  	
	        	} else {myWriter.write(ip_list[j]+";"); }
	        }
	        myWriter.write("\n");
	        for (int j = 0; j<2048; j++) {
	        	if (name_list[j]==null) {myWriter.write("null;");  	
	        	} else {myWriter.write(name_list[j]+";"); }
	        }
	        myWriter.write("\n");
	        for (int j = 0; j<2048; j++) {
	        	myWriter.write(index_list[j]+";");
	        }
	        myWriter.close();
			} catch (Exception e) {e.printStackTrace();}
			//Commits this to the git repository
			try {
				ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "Commit.bat");
				File dir = new File("I:\\git\\PeerToPeer");
				pb.directory(dir);
				Process p = pb.start();
			} catch (Exception e) {e.printStackTrace();}
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
			this.update = new int [3];
			update[0] = u[0];
			update[1] = u[1];
			update[2] = u[2];
		}
		
		public void run() {		    
			byte[] data = new byte[65536];
			if (update[1]!= -1) {
				try {
					String temp_data = "Update;"+update[0]+";"+update[1];
					data = temp_data.getBytes();
					for (int i =0;i<IP_list.length;i++) {
						if (port_list[i] != -1) {
							DatagramPacket packet = new DatagramPacket(data, data.length,IP_list[i],port_list[i]);
							socket_s.send(packet);
							packet = null;
						}
					}
				} catch (Exception e) {e.printStackTrace();}
			} else {
				try {
					String temp_data = "Close;"+update[0]+";"+update[2];
					data = temp_data.getBytes();
					for (int i =0;i<IP_list.length;i++) {
						if (port_list[i] != -1) {
							DatagramPacket packet = new DatagramPacket(data, data.length,IP_list[i],port_list[i]);
							socket_s.send(packet);
							packet = null;
						}
					}
				} catch (Exception e) {e.printStackTrace();}
			}
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
			byte[] data_node = null;		    
			try {
				//Gets the message and acts accordingly
				if ((message[0].trim()).equals("Update")) {
					synchronized(account_list) {account_list[Integer.parseInt(message[2].trim())] = Integer.parseInt(message[1].trim());}
					synchronized(account_index) {account_index[Integer.parseInt(message[2].trim())] = Integer.parseInt(message[2].trim());}
				} if ((message[0].trim()).equals("Close")) {
					synchronized(account_list) {account_list[Integer.parseInt(message[2].trim())] = 0;}
					synchronized(account_index) {account_index[Integer.parseInt(message[2].trim())] = -1;}
				}
				else if ((message[0].trim()).equals("New")) {
					//Updates its node list
					int n =Integer.parseInt(message[4].trim());
	 					synchronized(port_list) {port_list[n] = Integer.parseInt(message[1].trim());}
	 					synchronized(ip_list) {ip_list[n] = message[2].trim();	}
	 					synchronized(IP_list) {try {IP_list[n] = InetAddress.getByName(message[2].trim());
	 					} catch (Exception e) {}}
	 					synchronized(name_list) {name_list[n] = message[3].trim();	}
	 					synchronized(index_list) {index_list[n] = n;	}
		 			
		 		
		 			
					//If this is the node with initial contact, all nodes are updated by it
				} else if ((message[0].trim()).equals("Disconnect")) {
					//Updates its node list
					int n =Integer.parseInt(message[1].trim());
	 					synchronized(port_list) {port_list[n] = -1;}
	 					synchronized(ip_list) {ip_list[n] = null;	}
	 					synchronized(IP_list) {try {IP_list[n] = null;
	 					} catch (Exception e) {}}
	 					synchronized(name_list) {name_list[n] =null;}
	 					synchronized(index_list) {index_list[n] =-1;}
		 			
					
		 			
					//If this is the node with initial contact, all nodes are updated by it
				} else if ((message[0].trim()).equals("NewI")) {
						String temp = "New;"+message[1].trim()+";"+message[2].trim()+";"+message[3].trim()+";"+message[4].trim();
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
			 					synchronized(port_list) {port_list[i] = Integer.parseInt(message[1].trim());}
			 					synchronized(ip_list) {ip_list[i] = message[2].trim();}	
			 					synchronized(IP_list) {try {IP_list[i] = InetAddress.getByName(message[2].trim());
			 					} catch (Exception e) {e.printStackTrace();}}
			 					synchronized(name_list) {name_list[i] = message[3].trim();	}
			 					k = i;
			 					break;
			 				}
			 			}
			 			

			 			
				String account_dat = "";
				   for (int i = 0; i<2048; i++) {
				        if (account_index[i] != -1) {
				        	account_dat = account_dat +(account_list[i]+","+account_index[i]+";").trim();
				        }
				     }
				   data_node = account_dat.getBytes();
					DatagramPacket packet = new DatagramPacket(data_node, data_node.length,IP_list[k],port_list[k]);
					socket_m.send(packet);
					packet = null;
					
					//Reconstructs data
					try {
			        FileWriter myWriter = new FileWriter("Data.txt");
			        for (int j = 0; j<2048; j++) {
			        	myWriter.write(port_list[j]+";");
			        }
			        myWriter.write("\n");
			        for (int j = 0; j<2048; j++) {
			        	if (ip_list.equals(null)) {myWriter.write("null;");  	
			        	} else {myWriter.write(ip_list[j]+";");}
			        }
			        myWriter.write("\n");
			        for (int j = 0; j<2048; j++) {
			        	if (name_list.equals(null)) {myWriter.write("null;");  	
			        	} else {myWriter.write(name_list[j]+";");}
			        }
			        myWriter.write("\n");
			        for (int j = 0; j<2048; j++) {
			        myWriter.write(index_list[j]+";");
			        }
			        myWriter.close();
					} catch (Exception e) {e.printStackTrace();}
					
					//Commits this to the git repository
					try {
						ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "Commit.bat");
						File dir = new File("I:\\git\\PeerToPeer");
						pb.directory(dir);
						Process p = pb.start();
					} catch (Exception e) {e.printStackTrace();}
				} 
			
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
				String[] message;	
				String temp = new String(receive);
				message = temp.split(";");
				(new Messenger(message)).start();
				packet =null;
				//This is the message that the receiver will get after the client object has stopped 
				System.out.println(temp.trim());
				if (temp=="Disconnect;"+inds) {
					break;
				}
			}
			System.out.println("Disconnected");
		}
	}
	
 	//What the client sees
 	private class Client extends Thread{
 		//Request list same as the server except for create as this is for managing a specified account, also no disconnect for similar reasons
 		private static final String[] REQUEST_LIST = {"retreive","withdraw","deposit","close","exit"};
 		private static final String[] MENU_LIST = {"create","manage","disconnect"};
 		

 		public Client() {
 		}
 		
 		public void run() {	
 			/**Set everything up**/
 			Scanner myObj = new Scanner(System.in);
 	    	myObj.useDelimiter(System.lineSeparator());
	 	    boolean b = false;
	 		int money = 0;
	 		String money_str = null;
 			
 			/**NOW THE CLIENT CAN INTERACT WITH THE DATA**/
 			/**The menu which the user sees after loading the program**/
	 		boolean online = true;
 			while (online) {
 				String in = "";
 				int number;
 				//Outputs the menu
 				System.out.println("Please press: ");
 		    	for (int i=0; i<MENU_LIST.length; i++) {
 		    		int n = i+1;
 		    		System.out.println(n+": "+MENU_LIST[i]);
 		    		
 		    	}
 		    	//Takes input
 		    	System.out.print("Input: ");
 		    	try { 
 		    	boolean input =false;
 		    	in = myObj.next();
 		    	in.trim();
 		    	number = Integer.parseInt(in);
 				}
 				catch(Exception NumberFormatException) {
 				  number = 0;
 				}
 		    	//The account number
 				String account = "";
 				//b decides if its appropiate to show the managment menu
 				switch(number) {
 				  case 1:
 				    createAccount();
 				    b = false;
 				    break;
 				  case 2:
 					//Takes the input
 			    	System.out.print("Account number: ");
 	 		    	boolean input =false;
 		    		try {
 		    			account = myObj.next();
 		    		} catch (Exception e) {
 		    		}
 	 		    	
 			    	account.trim();  

 				    if (retrieveData(account)) {
 				    	b = true;
 				    }
 				   break;
 				  case 3:
 						byte[] dis;
 						System.out.println("Disconnecting... ");
 						String temp_data = "Disconnect;"+inds;
 						dis = temp_data.getBytes();
 						for (int i =0;i<IP_list.length;i++) {
 							if (port_list[i] != -1) {
 								DatagramPacket packet = new DatagramPacket(dis, dis.length,IP_list[i],port_list[i]);
 								try{socket_c.send(packet);
 								}catch (Exception e) {}	
 								packet = null;
 							}
 						}
 						
 						synchronized (port_list) {port_list[inds]=-1; }
 						synchronized(ip_list)  {ip_list[inds]=null; }
 						synchronized(IP_list)  {IP_list[inds]=null; }
 						synchronized(name_list)  {name_list[inds]=null; }
 						synchronized(index_list)  {index_list[inds]=-1; }	
						//Reconstructs data
						try {
				        FileWriter myWriter = new FileWriter("Data.txt");
				        for (int j = 0; j<2048; j++) {
				        	myWriter.write(port_list[j]+";");
				        }
				        myWriter.write("\n");
				        for (int j = 0; j<2048; j++) {
				        	if (ip_list.equals(null)) {myWriter.write("null;");  	
				        	} else {myWriter.write(ip_list[j]+";");}
				        }
				        myWriter.write("\n");
				        for (int j = 0; j<2048; j++) {
				        	if (name_list.equals(null)) {myWriter.write("null;");  	
				        	} else {myWriter.write(name_list[j]+";");}
				        }			
				        myWriter.write("\n");
				        for (int j = 0; j<2048; j++) {
				        	myWriter.write(index_list[j]+";");
				        }
				        myWriter.close();
						} catch (Exception e) {e.printStackTrace();}
 							

						try {
							ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "Commit.bat");
							File dir = new File("I:\\git\\PeerToPeer");
							pb.directory(dir);
							Process p = pb.start();
						} catch (Exception e) {e.printStackTrace();
 							
 							}
						b =false;
						online = false;
 					    break;
 				  default:
 					  System.out.println("Invalid input");
 					  b = false;
 					  break;
 				}
 				//If any changes were made in this menu they are updated here
 				number = 0;
 		    	if (change_index!=-1) {
 		    		int[] u = new int[3];
 		    		synchronized(account_list) { u[0] = account_list[change_index];}
 		    		synchronized(account_index) { u[1] = account_index[change_index];}
 		    		u[2] = change_index;
 		    		(new Updater(u)).start();
 		    		change_index = -1;
 		    	}
		    	
 				while (b) {
 	 	    	number = 0;
 	 	    	String choice = "";
 	 	    	System.out.println("Account "+account);
 	 	    	System.out.println("Please press: ");
 	 	    	for (int i=0; i<REQUEST_LIST.length; i++) {
 	 	    		int n = i+1;
 	 	    		System.out.println(n+": "+REQUEST_LIST[i]);
 	 	    		
 	 	    	}
 	 			try {
 	 		    	
	    			System.out.print("Input: ");
	    			choice = myObj.next();
 	 		    	
 	 		    	choice.trim();
 	 		    	number = Integer.parseInt(choice);
 	 				}
 	 				catch(Exception e) {
 	 				  number = 0;
 	 				}
 	 	    	switch(number) {
 	 	    	  case 1:
 	 	    		b = retrieveData(account);
 	 	    		break;
 	 	    	  case 2:
 	 	 		 	
 	 	 		 	//Here the user is asked to input there desired withdraw
	 	 			try {
	 	 				System.out.print("How much would you like to withdraw: ");
	 			    	money_str = myObj.next();
	 			    	money_str.trim();
	 			    	money= Integer.parseInt(money_str);
 	 	 			}
 	 	 			catch(Exception e) {
 	 	 				money = -1;
 	 	 				money_str = "-1";
 	 	 			}
 	 	 			
 	 	    		int w  = withdraw(account,money);
 	 	    		
 	 	 			
 	 	 			System.out.println("--------------------------");
 	 	 			if (w == -3) {
 	 	 				System.out.println("Insuffcient funds");
 	 	 				System.out.println("--------------------------");
 	 	 				System.out.println("");
 	 	 				change_index = -1;
 	 	 				b = true;
 	 	 				
 	 	 			} else if (w == -1) {
 	 	 				System.out.println("Inavlid input");
 	 	 				System.out.println("--------------------------");
 	 	 				System.out.println("");
 	 	 				change_index = -1;
 	 	 				b = true;
 	 	 			}else if (w == -2) {
 	 	 				System.out.println("Account no longer exists");
 	 	 				System.out.println("--------------------------");
 	 	 				System.out.println("");
 	 	 				change_index = -1;
 	 	 				b = false;
 	 	 			}else {
 	 	 				System.out.println("Account balance: " + account_list[Integer.parseInt(account)] );
 	 	 				System.out.println("--------------------------");
 	 	 				System.out.println("");
 	 	 				change_index = Integer.parseInt(account);
 	 	 				b = true;
 	 	 			}
 	 	    		break;
 	 	    	  case 3:
 	 	 		 	
 	 	 		 	//Here the user is asked to input there desired withdraw
 	 	 			try {
	 	 	 	    	System.out.print("How much would you like to deposit: ");
	 	 			    money_str = myObj.next();
	 	 	 	    	money_str.trim();
	 	 	 	    	money= Integer.parseInt(money_str);
 	 	 			}
 	 	 			catch(Exception e) {
 	 	 			 money = -1;
 	 	 			 money_str = "-1";
 	 	 			}
 	 	 			
 	 	 			
 	 	    		int d = deposit(account,money);
 	 	    		
 	 	 			System.out.println("");
 	 	 			System.out.println("--------------------------");
 	 	 			if (d == -1) {
 	 	 				System.out.println("Invalid Input");
 	 	 				System.out.println("--------------------------");
 	 	 				System.out.println("");
 	 	 				change_index = -1;
 	 	 				b = true;
 	 	 			}else if (d == -2) {
 	 	 				System.out.println("Account does not exist");
 	 	 				System.out.println("--------------------------");
 	 	 				System.out.println("");
 	 	 				change_index = -1;
 	 	 				b = false;
 	 	 				
 	 	 			} else {
 	 	 				System.out.println("Account balance: " + account_list[Integer.parseInt(account)]);
 	 	 				System.out.println("--------------------------");
 	 	 				System.out.println("");
 	 	 				change_index = Integer.parseInt(account);
 	 	 				b = true;
 	 	 			}
 	 	 			
 	 	    		
 	 	    		break;
 	 	    	  case 4:
 	 	      		closeData(account);
 	 	      		b = false;
 	 	      		break;
 	 	    	  case 5:
 	 	    		  b = false;
 	 	    		  break;
 	 	    	  default:
 	 	    		  System.out.println("Invalid request");
 	 	    		  b = true;
 	 	    		  break;
 	 	    	}
 	 	    		
 				number = 0;
 				//Any changes made in this menu is updated here on the network
 				System.out.println(change_index);
 		    	if (change_index!=-1) {
 		    		int[] u = new int[3];
 		    		synchronized(account_list) { u[0] = account_list[change_index];}
 		    		synchronized(account_index) { u[1] = account_index[change_index];}
 		    		u[2] = change_index;
 		    		(new Updater(u)).start();
 		    		change_index = -1;
 		    	}
 		    	
 				}
 				

 		    	
 				}
 		}
 		
 		/*private static boolean accountManagement(String account) {
 			Scanner myObj = new Scanner(System.in);
 	    	myObj.useDelimiter(System.lineSeparator());
 	    	int number = 0;
 	    	String choice = "";
 	    	System.out.println("Account "+account);
 	    	System.out.println("Please press: ");
 	    	for (int i=0; i<REQUEST_LIST.length; i++) {
 	    		int n = i+1;
 	    		System.out.println(n+": "+REQUEST_LIST[i]);
 	    		
 	    	}
 			try {
 		    	
 		    	boolean input =false;
 		    	while (!input) {
 		    		try {
 		    			System.out.print("Input: ");
 		    			choice = myObj.next();
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
 	    	return b;
 	    }
 		*/
 		private static boolean retrieveData(String account)  {
 			try {if (account_index[Integer.parseInt(account.trim())]!= -1) {
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
 	        
 		private static int withdraw(String account, int money)  {			
 			int balance = -1000;
 			if (account_index[Integer.parseInt(account)]==-1) {
 				try { balance = -2;
 				} catch (Exception e) {
 					balance = -1;
 				}
 		 	} else {
 		 		int temp = account_list[Integer.parseInt(account)];
 		 		synchronized (account_list) {if (0<=temp - money) {
 		 		temp -= money;
 		 		balance = temp;
 		 		account_list[Integer.parseInt(account)] = temp;} else {
 		 		
 		 		
 		 			balance = -3;
 		 		}}
 		 	}

 			return balance;
 		 	
 	 }
 	 
 	    private static  int deposit(String account, int money)  {

 			
 			int balance = -1000;
 			if (account_index[Integer.parseInt(account)]==-1) {
 				try { balance = -2;
 				} catch (Exception e) {
 					balance = -1;
 				}
 		 	} else {synchronized (account_list) { int temp = account_list[Integer.parseInt(account)];
 		 			temp += money;
 		 			balance = temp;
 		 			account_list[Integer.parseInt(account)] = temp;}}
 			
 			return balance;		 	
 	 }

 	    private static void createAccount()  {
 			boolean create = false;
			
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
 	 		account_index[ind] = ind;
 			System.out.println("Account number: " + ind);
 	    }
 	 
 	    private static void closeData(String account)  {
 			try {if (account_index[Integer.parseInt(account)]!= -1) {
 			System.out.println("--------------------------");
				change_index = account_index[Integer.parseInt(account)];
 				account_index[Integer.parseInt(account)] = -1;
 				account_list[Integer.parseInt(account)] = 0;

 			} else {
 				System.out.println("No account found");
 				System.out.println("--------------------------");
 				System.out.println("");
 				change_index = -1;
 			}
 			} catch (Exception e) {
 				System.out.println("Invalid input");
 				change_index = -1;
 			}
 	    }
 	    
 }
	
}