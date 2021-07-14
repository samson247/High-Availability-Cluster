import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A class implementing a peer in a high availability cluster
 * 
 * @author Sam Dodson
 *
 */
public class Peer {
	final static int PORT_NUMBER = 6000;
	private int port;
	private String address;
	private DatagramSocket serverSocket;
	private DatagramSocket clientSocket;
	private Timer timer = null;
	private ArrayList<PeerData> peerData = new ArrayList<>();
	private byte version = 1;
	private byte modePeerToPeer = 1;
	private byte codeAPPacket = 6;
	private int pulseCount = 0;
	private ArrayList<Integer> receivedFrom = new ArrayList<>();
	private Task task = new Task();
	private PeerData self;
	int id;
	boolean canBeSplit = false;
	boolean lastPacket = true;
	
	/**
	 * Constructor for Peer class
	 * @param address the IP address of the peer
	 */
	public Peer(String address) {
		this.port = PORT_NUMBER;
		this.address = address;
		createServerSocket();
		createClientSocket();
		readConfigAddresses();
	}
	
	/**
	 * Constructor for Peer class in local test mode
	 * @param port the port number of the peer, when ran in local mode
	 */
	public Peer(int port) {
		this.port = port;
		createServerSocket();
		createClientSocket();
		readConfigAddressesLocal();
	}
	
	/**
	 * Creates server socket of peer bound to specified port
	 */
	public void createServerSocket() {
		try {
			this.serverSocket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates client socket of peer
	 */
	public void createClientSocket() {
		try {
			this.clientSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
		
	/**
	 * Creates listening thread that runs in an infinite loop
	 */
	public void listen() {
		// Timer to send availability to peers is initialized and scheduled 
		// in run at a random interval from 0-30 seconds
		while(true) {
			if(timer == null) {
				timer = new Timer();
				createTimer();
			}
			
			// Availability packets are received from other peers and responded to in separate threads
			byte[] bytes = new byte[500];
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
			try {
				serverSocket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			createParseThread(packet);
		}
	}
		
	/**
	 * Availability of peer is sent to each peer in cluster
	 */
	public void sendAvailability() {
	    if(pulseCount > 0) {
	    	System.out.println("Heartbeat " + pulseCount);
	    }
	    byte[] packetBytes = getPacketData();
	    for(int index = 0; index < peerData.size(); index++) {
	    	DatagramPacket packet = null;
			packet = createPacket(packetBytes, peerData.get(index));
			sendPacket(packet);
			
			// Availability of nodes who sent packet are incremented
			if(pulseCount > 0) {
				if(receivedFrom.contains(peerData.get(index).getId())) {
					peerData.get(index).setAvailability(peerData.get(index).getAvailability() + 1);
					int rfIndex = receivedFrom.indexOf(peerData.get(index).getId());
					receivedFrom.remove(rfIndex);
				}
				printAvailability(index);
			}
			else {
				System.out.println("Address: " + peerData.get(index).getAddress() + " | First heartbeat");
			}
		}
		
	    System.out.println("\n");
	    pulseCount++;
	}
	
	/**
	 * Prints the availability of a given peer
	 * @param index the specified index number of a peer in the peerData list
	 */
	public void printAvailability(int index) {
		final int NODE_STATUS_CUTOFF = 30;
		double percentage = (peerData.get(index).getAvailability() / ((double)pulseCount)) * 100.0;
		if(percentage > NODE_STATUS_CUTOFF) {
			System.out.println("Address: " + peerData.get(index).getAddress() + " | Port: " + peerData.get(index).getPort() + " | Availability: " + peerData.get(index).getAvailability() + " (out of " + pulseCount + ")" + " | Percentage Available: " + String.format("%.2f", percentage) + " ALIVE");
		}
		else {
			System.out.println("Address: " + peerData.get(index).getAddress() + " | Port: " + peerData.get(index).getPort() + " | Availability: " + peerData.get(index).getAvailability() + " (out of " + pulseCount + ")" + " | Percentage Available: " + String.format("%.2f", percentage) + " DEAD");
		}
	}
				
	/**
	 * Packet data is added to stream and bytes are retrieved
	 * @return the bytes of packetData
	 */
	public byte[] getPacketData() {
		PacketData packetData = new PacketData(version, modePeerToPeer, codeAPPacket, self);
		packetData.setFlags(canBeSplit, lastPacket);
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream;
		try {
			objectStream = new ObjectOutputStream(byteArrayStream);
			objectStream.writeObject(packetData);
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] packetBytes = byteArrayStream.toByteArray();
		return packetBytes;
	}
	
	/**
	 * Availability packet is created and initialized with a peer's address and port number
	 * @param packetBytes the bytes of data to send in the packet
	 * @param peer the specified peer to send the packet to
	 * @return the packet initialized and ready to send
	 */
	public DatagramPacket createPacket(byte[] packetBytes, PeerData peer) {
		DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length, peer.getAddress(), peer.getPort());
		return packet;
	}
	
	/**
	 * Sends packet from client socket to a peer in the cluster
	 * @param packet the availability packet of this peer
	 */
	public void sendPacket(DatagramPacket packet) {
		try {
			clientSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	/**
	 * Creates a thread to parse received packet
	 * @param packet an availability packet received from a peer in the cluster
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
		
				// If packet fields match expected values than peer id is added to received from
				if(parsedVersion == version) {
					if(mode == modePeerToPeer) {
						if(code == codeAPPacket) {
							if(!receivedFrom.contains(packetData.getPeer().getId())) {
								receivedFrom.add(packetData.getPeer().getId());
							}	
						}
					}
				}
			}
		});
		thread.run();
	}
	
	/**
	 * Parses the data from the received availability packet
	 * @param packet the availability packet received from a peer in the cluster
	 * @return the data parsed from the packet
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
	 * Reads addresses from config file and adds peers to PeerData list
	 */
	public void readConfigAddresses() {
	    File configFile = new File("config.txt");
	    Scanner scanner = null;
		try {
			scanner = new Scanner(configFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		// Each line is read from file and parsed, peers are added to PeerData list
	    int idCounter = 0;
	    while (scanner.hasNextLine()) {
	    	String address = scanner.nextLine();
	        PeerData peer = null;
			try {
				peer = new PeerData(InetAddress.getByName(address), PORT_NUMBER, idCounter, 0);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
	        peerData.add(peer);
	        
	        if(this.address.equals(address)) {
	        	this.id = idCounter;
	        	this.self = peer;
	        }
	        idCounter++;
	      }
	    scanner.close();
	}
	
	/**
	 * Reads addresses and port numbers from local config file, used only when in local test mode
	 */
	public void readConfigAddressesLocal() {
	    File configFile = new File("configLocal.txt");
	    Scanner scanner = null;
		try {
			scanner = new Scanner(configFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		// Each line is read from file and parsed, peers are added to PeerData list
	    int idCounter = 0;
	    while (scanner.hasNextLine()) {
	    	String fileData = scanner.nextLine();
	    	String [] nodeData = fileData.split("\\s+");
	        PeerData peer = null;
			try {
				peer = new PeerData(InetAddress.getByName(nodeData[0]), Integer.parseInt(nodeData[1]), idCounter, 0);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
	        peerData.add(peer);
	        if(Integer.parseInt(nodeData[1]) == port) {
	        	this.id = idCounter;
	        	this.self = peer;
	        }
	        idCounter++;
	      }
	    scanner.close();
	}
	
	/**
	 * Timer is initialized to run at interval from 0-30 seconds
	 */
	public void createTimer() {
		Random rand = new Random();
	    int upperbound = 31;
	    int delay = rand.nextInt(upperbound) * 1000;
		timer.schedule(task, delay);
	}
	
	/**
     * Class implementing task that sends availability to peers every 0-30 seconds
     * @author Sam Dodson
     *
     */
	public class Task extends TimerTask {
        /**
         * Runs every 0-30 seconds and sends availability to each other node
         */
		@Override
        public void run() {
        	sendAvailability();
        	
        	// Timer is rescheduled to run at a random interval
			Random rand = new Random();
		    int upperbound = 31;
		    int delay = rand.nextInt(upperbound) * 1000;
		    timer.schedule(new Task(), delay);
        }
    }
}
