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
 * Directory, acts as a MetaAgent that others can connect to and receive a list of other meta agents
 * @author Group B
 */
public class Directory extends MetaAgent
{

    /**
     * The list of connections the directory currently holds.
     */
    protected HashMap<String, Connection> connections = new HashMap<>();

    /**
     * Constructor to give the directory an identifying string.
     *
     * @param handle Used to pair it with its connection around the system.
     */
    public Directory(String handle)
    {
        super(handle, DEFAULT_RECV_IP_ADDRESS, DEFAULT_PORT);
    }

    /**
     * Constructor with string handle, along with the IP string of its
     * connection.
     *
     * @param handle Used to pair it with its connection around the system.
     * @param receiveIp
     */
    public Directory(String handle, String receiveIp)
    {
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
    public Directory(String handle, String receiveIp, int receivePort)
    {
        super(handle, receiveIp, receivePort);
    }

    /**
     * Removes all current connections from the directory.
     */
    @Override
    public void removeConnections()
    {
        connections = new HashMap<>();
    }

    /**
     * Thread which will accept messages from other connections in the system.
     */
    protected Thread acceptThread = new Thread(
            new Runnable()
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    final Socket newClientSocket = serverSocket.accept();

                    //Create a partial connection
                    final Connection newConnection = new Connection(newClientSocket);

                    while (!newConnection.hasMessage())
                    {
                        // wait for a message from the new connection...
                        // should probably handle timeouts...
                    }

                    //Waits for a PORTAL message to connect to
                    final Message receivedMessage = newConnection.receiveMessage();

                    if (receivedMessage.getType().equals(MessageType.PORTAL))
                    {
                        final String newConnectionHandle = receivedMessage.getFrom();

                        if (newConnectionHandle != null)
                        {
                            synchronized (lock)
                            {

                                if (connections.get(newConnectionHandle) == null)
                                {
                                    //Set the connection handle, used for reference for messages
                                    newConnection.setHandle(newConnectionHandle);

                                    //update our register of peer connections
                                    addConnection(newConnection);

                                    //The HELLOACK allows the peer to know our handle
                                    newConnection.sendMessage(createDirMessage(newConnectionHandle));
                                }
                                else
                                {
                                    //remove any mathing handle and replace with this, acts as a refresh
                                    connections.remove(newConnectionHandle);
                                    newConnection.setHandle(newConnectionHandle);
                                    addConnection(newConnection);

                                    connections.values().forEach((c) ->
                                    {
                                        c.sendMessage(createDirMessage(c.getHandle()));
                                    });
                                }
                            }
                        }
                    }
                    else
                    {
                        System.err.println("Invalid Portal connect message, connection attempt will be dropped.");
                    }
                }
                catch (IOException ex)
                {
                    Logger.getLogger(MetaAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }
    );

    /**
     * Creates message object with current directory connections.
     *
     * @param from The directory sending the message.
     * @param to The message receiver.
     * @return Single string of all directory's current IP addresses.
     */
    private Message createDirMessage(String to)
    {
        Message m = new Message(handle, to, MessageType.DIR);
        String content = "";
        for (Connection c : connections.values())
        {
            Pattern ipPattern = Pattern.compile("(?<=/)(.*?)(?=,)");
            Matcher match = ipPattern.matcher(c.getSocket().toString());

            String ip;

            if (match.find())
            {
                ip = match.group();
            }
            else
            {
                ip = "";
            }
            
            //Checks if the ip is the ip of the requesting node
            //If it is, don't add it to the list
            if (!getIp(connections.get(to).getSocket()).equals(ip))
            {
                content += match.group();
            }
        }
        m.append(content);
        return m;
    }

    /**
     * Returns the ip of a socket
     * @param s The socket whose IP is needed
     * @return the IP of the Socket
     */
    private String getIp(Socket s)
    {
        return s.getInetAddress().toString().substring(1);
    }

    /**
     * Starts the directory's ability to recieve new peer connections.
     *
     * @throws IOException Handles errors with input/output errors.
     */
    @Override
    public void begin() throws IOException
    {
        startPeerReceiver();
    }

    /**
     * Checks if directory holds any current connections.
     *
     * @return If size of connection list is more than 0
     */
    public synchronized boolean hasConnections()
    {
        return connections.size() > 0;
    }

    /**
     * Returns list of all identifying strings inside connection list.
     *
     * @return LinkedList which holds all keys inside connections list.
     */
    public synchronized List<String> getConnectionHandles()
    {
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
    protected void startPeerReceiver() throws UnknownHostException, IOException
    {
        if (serverSocket == null)
        {
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
    private void addConnection(final Connection connection)
    {
        synchronized (lock)
        {
            if (connections.containsKey(connection.getHandle()))
            {
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
    protected synchronized boolean isalreadyConnected(final String ipAddress)
    {
        for (Connection c : connections.values())
        {
            if (c.hasIpAddress(ipAddress))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all IP addresses in current connections list.
     *
     * @return String which contains all stored IPs.
     */
    public String getAddresses()
    {
        String output = "";
        for (Connection c : connections.values())
        {

            output += c.getSocket().toString().substring(13, 27) + ",";
        }
        return output;
    }
}
