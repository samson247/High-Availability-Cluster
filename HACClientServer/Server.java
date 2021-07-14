import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A class to model the data and functionalities of a server in a high-availability cluster
 * @author Sam Dodson
 *
 */
public class Server {
	private int port;
	private DatagramSocket socket;
	private ArrayList<ClientData> clientData = new ArrayList<>();
	private ArrayList<ClientData> liveNodes = new ArrayList<>();
	private Timer timer = null;
	private byte version = 1;
	private byte modeClientServer = 0;
	private byte codeHCPacket = 0;
	private byte codeHSPacket = 1;
	private byte codeACPacket = 2;
	private byte codeASPacket = 3;
	private int clientCount = 0;
	
	/**
	 * Constructor for Server class, sets port to bind socket to
	 * @param port
	 */
	public Server(int port) {
		this.port = port;
	}
	
	/**
	 * Runs handshake protocol and begins listening for incoming packets
	 */
	public void handshake() {
		createSocket();
		listen();
	}
	
	/**
	 * Creates socket bound to specified port
	 */
	public void createSocket() {
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			// Program exits if the port is already bound to another program
			System.out.println("Port already bound to socket.");
			System.exit(-1);
		}
	}
	
	/**
	 * Main listening thread of server node
	 */
	public void listen() {
		while(true) {
			byte[] bytes = new byte[1024];
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Separate thread is created to parse each incoming packet
			createParseThread(packet);
			
			// Timer is started to send heart beat to client nodes every 30 seconds
			if(timer == null) {
				timer = new Timer();
				createTimer();
			}
		}
	}
	
	/**
	 * Creates and runs a thread to parse incoming packets
	 * @param packet the packet from client to parse
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
				
				if(parsedVersion == version) {
					if(mode == modeClientServer) {
						if(code == codeHCPacket) {
							int id = packetData.getClientData().get(0).getId();
							// If client already has an id check if they are reconnecting
							if(id != -1) {
								try {
									int index = clientData.indexOf(packetData.getClientData().get(0));
									clientData.get(index).setAddress(packet.getAddress());
									clientData.get(index).setPort(packet.getPort());
									sendPacket(packet.getAddress(), packet.getPort(), clientData.get(index), codeHSPacket);
								}
								catch(IndexOutOfBoundsException e) {
									// If client had id from previous server reassign a new id
									ClientData client = new ClientData(packet.getAddress(), packet.getPort(), 0, clientCount);
									clientData.add(client);
									clientCount += 1;
									sendPacket(packet.getAddress(), packet.getPort(), client, codeHSPacket);
								}
							}
							else {
								// If new client add them and assign an id
								ClientData client = new ClientData(packet.getAddress(), packet.getPort(), 0, clientCount);
								clientData.add(client);
								clientCount += 1;
								sendPacket(packet.getAddress(), packet.getPort(), client, codeHSPacket);
							}
						}
						else if(code == codeACPacket) {
							// Add clients that send availability to live node list
							liveNodes.add(packetData.getClientData().get(0));	
						}
					}
				}
			}
		});
		thread.run();
	}
		
	/**
	 * Parses data from packet
	 * @param packet the packet from client to parse
	 * @return the data parsed from packet
	 */
	public PacketData parsePacket(DatagramPacket packet) {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(packet.getData());
		ObjectInputStream objectStream;
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
	 * Creates packet to send to specified client
	 * @param address the IP address of a client
	 * @param port the port number of a client
	 * @param nodeData the data to send in the packet
	 * @param code the type of packet being sent
	 * @return the packet created with the specified parameters
	 */
	public DatagramPacket createPacket(InetAddress address, int port, ClientData nodeData, byte code) {
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = null;
		byte[] packetBytes = null;
		try {
			objectStream = new ObjectOutputStream(byteArrayStream);
			PacketData packetData = null;
			
			// Creates a handshake packet or availability packet
			if(code == codeHSPacket) {
				ArrayList<ClientData> client = new ArrayList<>();
				client.add(nodeData);
				packetData = new PacketData(version, modeClientServer, codeHSPacket, client);
				packetData.setFlags(false, true);
			}
			else if(code == codeASPacket) {
				packetData = new PacketData(version, modeClientServer, codeASPacket, clientData);
				packetData.setFlags(true, true);
			}
			objectStream.writeObject(packetData);
			packetBytes = byteArrayStream.toByteArray();
			objectStream.close();
			byteArrayStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length, address, port);
		return packet;
	}
		
	/**
	 * Sends packet to specified client
	 * @param address the IP address of a client
	 * @param port the port number of a client
	 * @param client the data to send in the packet
	 * @param code the type of packet being sent
	 */
	public void sendPacket(InetAddress address, int port, ClientData client, byte code) {
		DatagramPacket packet = createPacket(address, port, client, code);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
						
	/**
	 * Creates timer to send heart beat to clients every 30 seconds
	 */
	public void createTimer() {
		int thirtySeconds = 30 * 1000;
		timer.schedule( new TimerTask() {
		    public void run() {
		    	// Availability is incremented for each live client
		    	for(ClientData client: clientData) {
		    		if(liveNodes.contains(client)) {
		        		int av = client.getAvailability();
		    			client.setAvailability(av + 1);
		    			client.setStatus(false);
		    		}
		    		else {
		    			client.setStatus(true);
		    		}
		    	}
		    	
		    	// Availability is sent to each live client
		    	for(ClientData client: clientData) {
		    		if(liveNodes.contains(client)) {
						sendPacket(client.getAddress(), client.getPort(), null, codeASPacket);
		    		}
					if(client.getStatus() == false) {
						System.out.println("Address: " + client.getAddress() + " | Port: " + client.getPort() + " | Availability: " + client.getAvailability() + " | Status: ALIVE");
					}
					else {
						System.out.println("Address: " + client.getAddress() + " | Port: " + client.getPort() + " | Availability: " + client.getAvailability() + " | Status: DEAD");
					}
		    	}
		    	System.out.println("\n");
		    	liveNodes.clear();
		    }
		 }, thirtySeconds, thirtySeconds);
	}
}
