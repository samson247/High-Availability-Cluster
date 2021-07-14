/**
 * Node class for HAC protocol that runs as either a client or server node
 * @author Sam Dodson
 *
 */
public class Node {
	private Server server;
	private Client client;
	private boolean isClient = true;
	
	/**
	 * Constructor for server node 
	 * @param port the port number to bind the server socket to
	 */
	Node(int port) {
		client = null;
		server = new Server(6000);
	}
	
	/**
	 * Constructor for client node
	 * @param ip the IP address of the server
	 * @param port the port number of the server
	 */
	Node(String ip, int port) {
		server = null;
		client = new Client(ip, port);
	}
	
	/**
	 * Getter for client field
	 * @return the value of client field
	 */
	public Client getClient() {
		return this.client;
	}
	
	/**
	 * Getter for server field
	 * @return the value of server field
	 */
	public Server getServer() {
		return this.server;
	}
	
	/**
	 * Returns true or false depending on if node is a client
	 * @return if a node is a client
	 */
	public boolean getIsClient() {
		return this.isClient;
	}
	
	/**
	 * Sets a node as a client or server
	 * @param value true or false depending on if a node is a client
	 */
	public void setIsClient(boolean value) {
		this.isClient = value;
	}
		
	/**
	 * Promotes client to server by setting client to null and creating you server object
	 */
	public void promoteToServer() {
		System.out.println("Promoted to server\n");
		client = null;
		server = new Server(6000);
		server.handshake();
	}
}
