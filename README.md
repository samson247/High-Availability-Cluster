# High-Availability-Cluster
A high availability cluster implemented in Java

Sam Dodson
High Availability Cluster Protocol
Packet Structure:
| Version | Mode | Code | Flags | Data |

Version: A 1-byte field representing the version of the protocol that produced the packet 

Mode: A 1-byte field representing the mode, either Client-Server or Peer-to-Peer

Code: A 1-byte field representing a code that specifies the type of packet, either HC, HS, AC, AS, or AP
  - HC: A packet sent from the client to the server to initiate a handshake. This is the first packet sent when a client joins the cluster.
  - HS: A packet sent from the server to a client as a response to the HC packet. This packet contains a client’s assigned id number as part of the data field.
  - AC: This is the availability packet sent from a client to the server. It contains one client’s data.
  - AS: This is the availability packet sent from the server to every live client node. It contains the availability data of all the client nodes.
  - AP: This is the availability packet sent from a peer node to every other peer node in the cluster.

Flags: A 1-byte field with 8 1-bit flags. Two of the flags are in use and the rest are reserved for future use.
  - canBeSplit: A true (1) or false (0) value depending on whether a given packet has to be split to be received at the proper node. This flag would come into use       when a cluster is too large for the server to send the availability of all nodes in one packet.
  - lastPacket: A true (1) or false (0) value signaling if a packet is the last to be received when multiple packets are being sent. This flag would come into use       when a cluster is too large for the server to send the availability of all nodes in one packet.

Data: This is a variable length field that represents the data or payload being transmitted by the protocol. It will always be a list of availability data either consisting of client data or peer data depending on the mode currently running.

Client-Server Version:
To run this version as a server you must pass a command line argument corresponding to the port number to bind the server socket to. To run this version as a client you must pass the IP address and port number of the server. The main class is the ProtocolDriver class.

Peer-to-Peer Version:
To run this version you must pass the IP address of the peer node that is joining the cluster. This IP address must correspond to an address in the configuration file. (To run a local test version you must pass IP address and port number as well). The main class is the ProtocolDriver class.
