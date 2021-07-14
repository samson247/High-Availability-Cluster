import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * A class to model the data and functionalities of a client in a high-availability cluster
 * @author Sam Dodson
 *
 */
public class Client {
	private InetAddress serverIp;
	private int serverPort;
	private DatagramSocket socket;
	private DatagramPacket packet;
	private Boolean serverAlive = false;
	private ArrayList<ClientData> clientData = new ArrayList<>();
	private byte version = 1;
	private byte modeClientServer = 0;
	private byte codeHCPacket = 0;
	private byte codeHSPacket = 1;
	private byte codeACPacket = 2;
	private byte codeASPacket = 3;
	private int idNumber = -1;
	private ClientData self;
	private int timeoutCount = 0;
	boolean canBeSplit = false;
	boolean lastPacket = true;
	
	/**
	 * The constructor for the Client class
	 * @param serverIp the IP address of the server node to send data to and receive data from
	 * @param serverPort the port number the server node is listening on
	 */
	public Client(String serverIp, int serverPort) {
		try {
			this.serverIp = InetAddress.getByName(serverIp);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.serverPort = serverPort;
		self = null;
	}
	
	/**
	 * A method to run the handshake part of the protocol
	 */
	public void handshake() {
		createSocket();
		readIdFile();
		createPacket(codeHCPacket);
		sendPacket();
		listenForResponse();
	}
	
	/**
	 * A method to run the main part of the protocol. It will continue to run unless the server 
	 * goes down and the client socket times out.
	 */
	public void protocol() {
		createPacket(codeACPacket);
		waitToSend();
		sendPacket();
		listenForResponse();
	}
	
	/**
	 * Creates a client socket to send and receive data
	 */
	public void createSocket() {
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a packet depending on the code it is passed
	 * @param code the packet code either for the handshake or availability packet
	 */
	public void createPacket(byte code) {
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream;
		byte[] packetBytes = null;
		try {
			// The packet is comprised of data from a PacketData object that is written to a stream
			objectStream = new ObjectOutputStream(byteArrayStream);
			ArrayList<ClientData> dataField = new ArrayList<>();
			dataField.add(self);
			PacketData packetData = new PacketData(version, modeClientServer, code, dataField);
			packetData.setFlags(canBeSplit, lastPacket);
			objectStream.writeObject(packetData);
			
			// The stream is converted to a byte array and will be sent in this format
			packetBytes = byteArrayStream.toByteArray();
			byteArrayStream.close();
			objectStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		this.packet = new DatagramPacket(packetBytes, packetBytes.length, serverIp, serverPort);
	}
		
	/**
	 * Sends a packet to the server node
	 */
	public void sendPacket() {
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	/**
	 * Delays a packet from being sent for 0-30 seconds
	 */
	public void waitToSend() {
		Random rand = new Random();
	    int upperbound = 31;
	    int random = rand.nextInt(upperbound) * 1000;
	    try {
			Thread.sleep(random);
	    } catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
				
	/**
	 * Listens for a response packet from the server. The socket will timeout after 31 seconds without a response.
	 */
	public void listenForResponse() {
		byte[] bytes = new byte[1024];
		DatagramPacket response = new DatagramPacket(bytes, bytes.length);
		try {
			socket.setSoTimeout(31 * 1000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		try {
			socket.receive(response);
			createParseThread(response);
		}
		catch(SocketTimeoutException e) {
			// Handshake with server will be done again and if timeout occurs a second 
			//time promotion sequence will take place
			if(timeoutCount == 0) {
				timeoutCount++;
				handshake();
			}
			else {
				serverAlive = false;
				timeoutCount = 0;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
			
	/**
	 * Creates and runs a thread to parse a received packet
	 * @param packet
	 */
	public void createParseThread(DatagramPacket packet) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				PacketData packetData = null;
				packetData = parsePacket(packet);
				byte parsedVersion = packetData.getVersion();
				byte mode = packetData.getMode();
				byte code = packetData.getCode();
				
				if(version == parsedVersion) {
					if(mode == modeClientServer) {
						if(code == codeHSPacket) {
							// Id number is retrieved and written to file
							idNumber = packetData.getClientData().get(0).getId();
							
							// Self will be passed to server in future availability packets
							self = packetData.getClientData().get(0);
							serverAlive = true;
							
							writeFile();
							return;
						}
						else if(code == codeASPacket) {
							// Availability data for each client is printed
							clientData = packetData.getClientData();
							for(ClientData client: clientData) {
								if(client.getStatus() == false) {
									System.out.println("Address: " + client.getAddress() + " | Port: " + client.getPort() + " | Availability: " + client.getAvailability() + " | Status: ALIVE");
									//System.out.printf("Client address: %s %d %d ALIVE%n", client.getAddress(), client.getPort(), client.getAvailability());
								}
								else {
									System.out.println("Address: " + client.getAddress() + " | Port: " + client.getPort() + " | Availability: " + client.getAvailability() + " | Status: DEAD");
									//System.out.printf("Client address: %s %d %d ALIVE", client.getAddress(), client.getPort(), client.getAvailability());
								}
							}
							System.out.println("\n");
						}
					}
				}
			}
		});
		thread.run();
	}
	
	/**
	 * Parses packet data from various streams
	 * @param packet the packet to be parsed
	 * @return the data parsed from the packet
	 */
	public PacketData parsePacket(DatagramPacket packet) {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(packet.getData());
		ObjectInputStream objectStream = null;
		PacketData packetData = null;
		try {
			objectStream = new ObjectInputStream(inputStream);
			packetData = (PacketData) objectStream.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return packetData;
	}
	
	/**
	 * Reads the id file containing the id and IP of the server who assigned it
	 */
	public void readIdFile() {
		// Self is initialized and will be assigned the id from the file
		self = new ClientData(null, 0, 0, -1);
	    File idFile = new File("id.txt");
	    if(idFile.exists()) {
		    Scanner scanner = null;
			try {
				scanner = new Scanner(idFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			String fileData = scanner.nextLine();
	    	String [] data = fileData.split("\\s+");
	    	try {
	    		// If same server is still active than proceed otherwise a new id is needed
				if(serverIp.toString().equals(data[1])) {
					String idString = data[0];
					self.setId(Integer.parseInt(idString));
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		    scanner.close();
	    }
	}
	
	/**
	 * Writes id given by server to file along with server IP
	 */
	public void writeFile() {
		try {
			File file = new File("id.txt");
		    if(file.createNewFile()) {
		        FileWriter writer = new FileWriter("id.txt");
		        writer.write(String.valueOf(idNumber) + " " + serverIp.toString());
		        writer.close();
		      }
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	}
	
	/**
	 * Gives status of server
	 * @return the current status of server
	 */
	public Boolean getServerAlive() {
		return this.serverAlive;
	}
	
	/**
	 * Determines which client node will be promoted to server based on the availability scores
	 * @return true or false depending on if the client is the new server
	 */
	public Boolean promoteToServer() {
		// Returns true if no handshake was ever performed
		if(idNumber == -1) {
			return true;
		}
		
		// Stale id file is deleted
		File idFile = new File("id.txt");
		idFile.delete();
		
		int maxAv = 0;
		ClientData newServer = null;
		for(ClientData client: clientData) {
			// New server is determined based on max availability and status being alive
			if(client.getAvailability() > maxAv && client.getStatus() == false) {
				newServer = client;
				maxAv = newServer.getAvailability();
			}
		}
		
		// Remove new server in case another timeout occurs
		if(newServer != null) {
			int index = clientData.indexOf(newServer);
			clientData.remove(index);
		}
		else {
			// Occurs when handshake occurred but no availability packets were received
			// Requires reconnection with new server
			System.out.println("Server failure. Reconnect.");
			System.exit(-1);
		}
			
		// Returns true if self was selected as new server
		// Otherwise serverIp is set and handshake will be attempted after waiting 10 seconds
		if(newServer.getId() == idNumber) {
			return true;
		}
		else {
			serverIp = newServer.getAddress();
		    try {
		    	int tenSeconds = 10 * 1000;
				Thread.sleep(tenSeconds);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
}
