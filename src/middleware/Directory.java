/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package middleware;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author s6089488
 */
public class Directory extends ChatNode {

    //Messages are sent as a client.
    //
    /**
     * The list of connections the directory currently holds.
     */
    protected HashMap<String, Connection> connections = new HashMap<>();

    /**
     * Constructor to give the directory an identifying string.
     *
     * @param handle Used to pair it with its connection around the system.
     */
    public Directory(String handle) {
        super(handle, DEFAULT_RECV_IP_ADDRESS, DEFAULT_PORT);
    }

    /**
     * Constructor with string handle, along with the IP string of its
     * connection.
     *
     * @param handle Used to pair it with its connection around the system.
     * @param receiveIp
     */
    public Directory(String handle, String receiveIp) {
        super(handle, receiveIp, DEFAULT_PORT);
    }

    /**
     * Constructor with string handle, string recieveIp, and the integer port
     * number. of the directory.
     *
     * @param handle Used to identify the connection around the system.
     * @param receiveIp Used to locate the directory around the system.
     * @param receivePort Used to locate the port of the connection.
     */
    public Directory(String handle, String receiveIp, int receivePort) {
        super(handle, receiveIp, receivePort);
    }

    /**
     * Removes all current connections from the directory.
     */
    @Override
    public void removeConnections() {
        connections = new HashMap<>();
    }

    /**
     * Thread which will accept messages from other connections in the system.
     */
    protected Thread acceptThread = new Thread(
            new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    final Socket newClientSocket = serverSocket.accept();

                    //Create a partial connection
                    final Connection newConnection = new Connection(newClientSocket);

                    System.out.println("Awaiting PORTAL message from new connection");

                    while (!newConnection.hasMessage()) {
                        // wait for a message from the new connection...
                        // should probably handle timeouts...
                    }

                    //At this point in the connection process, only a HELLO message
                    //will do, anything else will be ignored.
                    //
                    final Message receivedMessage = newConnection.receiveMessage();

                    System.out.println("Message received: " + receivedMessage.toString());

                    if (!(receivedMessage.getType().equals(MessageType.PORTAL))) {
                        System.err.println("Invalid Portal connect message, connection attempt will be dropped.");
                    } else {
                        final String newConnectionHandle = receivedMessage.getFrom();

                        if (newConnectionHandle != null) {
                            synchronized (lock) {

                                if (connections.get(newConnectionHandle) == null) {
                                    //Complete the connection by setting its handle.
                                    //this is essential as we use the handle to send
                                    //messages to our peers.
                                    //
                                    newConnection.setHandle(newConnectionHandle);

                                    //update our register of peer connections
                                    //
                                    addConnection(newConnection);

                                    //The HELLOACK allows the peer to know our handle
                                    //
                                    newConnection.sendMessage(createDirMessage(handle, newConnectionHandle));
                                } else {
                                    connections.remove(newConnectionHandle);
                                    newConnection.setHandle(newConnectionHandle);
                                    addConnection(newConnection);
                                    for (Connection c : connections.values()) {
                                        c.sendMessage(createDirMessage(handle, c.getHandle()));
                                    }
                                    //newConnection.sendMessage(createDirMessage(handle, newConnectionHandle));

                                }
                            }
                        }
                    }
                    // Check for HELLO message with client name.

                } catch (IOException ex) {
                    Logger.getLogger(ChatNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }
    );

    /**
     * Creates message object with current directory connections.
     *
     * @param from The directory sending the message.
     * @param to The message reciever.
     * @return Single string of all directory's current IP addresses.
     */
    private Message createDirMessage(String from, String to) {
        Message m = new Message(from, to, MessageType.DIR);
        String content = "";
        for (Connection c : connections.values()) {
            Pattern ipPattern = Pattern.compile("(?<=\\/)(.*?)(?=,)");
            System.out.println(c.socket.toString());
            Matcher match = ipPattern.matcher(c.socket.toString());

            System.out.println("found " + match.group());

            content += match.group();

        }
        m.append(content);
        return m;
    }

    /**
     * Starts the directory's ability to recieve new peer connections.
     *
     * @throws IOException Handles errors with input/output errors.
     */
    @Override
    public void begin() throws IOException {
        startPeerReceiver();
    }

    /**
     * Checks if directory holds any current connections.
     *
     * @return If size of connection list is more than 0
     */
    public synchronized boolean hasConnections() {
        return connections.size() > 0;
    }

    /**
     * Returns list of all identifying strings inside connection list.
     *
     * @return LinkedList which holds all keys inside connections list.
     */
    public synchronized List<String> getConnectionHandles() {
        List<String> handles = new LinkedList<>();
        handles.addAll(connections.keySet());
        return handles;
    }

    /**
     * Starts directory's ability to recieve new peer connections by starting an
     * instance of Thread acceptThread.
     *
     * @throws UnknownHostException Handles errors of unknown IP addresses.
     * @throws IOException Handles errors with input/output errors.
     */
    @Override
    protected void startPeerReceiver() throws UnknownHostException, IOException {
        if (serverSocket == null) {
            InetAddress bindAddress = InetAddress.getByName(this.receiveIp);
            serverSocket = new ServerSocket(this.receivePort, 0, bindAddress);
            acceptThread.start();
        }
    }

    /**
     * Adds new connection to the directory's current connection list.
     *
     * @param connection Connection to be added.
     */
    protected void addConnection(final Connection connection) {
        synchronized (lock) {
            if (connections.containsKey(connection.getHandle())) {
                System.err.println("[" + connection.getHandle() + "] is already an established connection.");
                return;
            }
            connections.put(connection.getHandle(), connection);
        }
    }

    /**
     * Checks if directory is already connected to an IP address.
     *
     * @param ipAddress IP to be checked.
     * @return If IP address is found in connections list.
     */
    @Override
    protected synchronized boolean isalreadyConnected(final String ipAddress) {
        for (Connection c : connections.values()) {
            if (c.hasIpAddress(ipAddress)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends message to a recipient.
     *
     * @param message Message to be sent.
     */
    @Override
    public void sendMessage(Message message) {
        synchronized (lock) {
            if (message.getType().equals(MessageType.BROADCAST)) {
                //
                // Not handling broadcast messages presently...
                //
            } else {
                final String receiver = message.getTo();

                //find the socket of the peer using their handle:
                Connection peerConnection = connections.get(receiver);

                if (peerConnection != null) {
                    peerConnection.sendMessage(message);
                } else {
                    System.err.println("'" + receiver + "' is an unknown peer");
                }

            }
        }
    }

    /**
     * Gets all IP addresses in current connections list.
     *
     * @return String which contains all stored IPs.
     */
    public String getAddresses() {
        String output = "";
        for (Connection c : connections.values()) {
            System.out.println(c.socket.toString().substring(13, 27));

            output += c.socket.toString().substring(13, 27) + ",";
        }
        return output;
    }
}
