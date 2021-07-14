/**
 * Driver class for protocol 
 * @author Sam Dodson
 *
 */
public class ProtocolDriver {
	/**
	 * Main method for class
	 * @param args the IP address and port number of server for client node
	 * or port number for server node
	 */
	public static void main(String[] args) {
		// Runs when a client node is created
		if(args.length == 2) {
			try {
				Node node = new Node(args[0], Integer.parseInt(args[1]));
				
				// Client logic is run while server is alive
				while(node.getIsClient()) {
					node.getClient().handshake();
					while(node.getClient().getServerAlive()) {
						node.getClient().protocol();
					}
					
					Boolean promote = node.getClient().promoteToServer();
					
					// Client is promoted to server if necessary
					if(promote) {
						node.promoteToServer();
						node.setIsClient(false);
					}
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		else if(args.length == 1) {
			// Runs when the server node is created
			try {
				Node node = new Node(Integer.parseInt(args[0]));
				node.getServer().handshake();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}	
	}
}
