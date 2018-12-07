/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peertopeer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author s6089488
 */
public class Directory
{
    protected final Object lock = new Object();

    protected static final String DEFAULT_RECV_IP_ADDRESS = "127.0.0.1";
    protected static final int DEFAULT_PORT = 9090;

    //Messages are received as a server, other peers need to connect.
    //
    protected ServerSocket serverSocket;
    protected String receiveIp;
    protected String handle;
    protected int receivePort;
    
    //Messages are sent as a client.
    //
    protected HashMap<String, Connection> peerGroupConnections = new HashMap<>();

    
    public Directory(String handle)
    {
        this(handle, DEFAULT_RECV_IP_ADDRESS, DEFAULT_PORT);
    }

    public Directory(String handle, String receiveIp)
    {
        this(handle, receiveIp, DEFAULT_PORT);
    }

    public Directory(String handle, String receiveIp, int receivePort)
    {
        this.handle = handle;
        this.receiveIp = receiveIp;
        this.receivePort = receivePort;
    }
    
    public void removeConnections()
    {
        peerGroupConnections = new HashMap<>();
    }

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

                    System.out.println("Awaiting HELLO message from new connection");

                    while (!newConnection.hasMessage())
                    {
                        // wait for a message from the new connection...
                        // should probably handle timeouts...
                    }

                    //At this point in the connection process, only a HELLO message
                    //will do, anything else will be ignored.
                    //
                    final Message receivedMessage = newConnection.receiveMessage();

                    System.out.println("Message received: " + receivedMessage.toString());
                    
                    if (!receivedMessage.isHelloMessage())
                    {
                        System.err.println("Malformed peer HELLO message, connection attempt will be dropped.");
                    }
                    else
                    {
                        final String newConnectionHandle = receivedMessage.getFrom();

                        if (newConnectionHandle != null)
                        {
                            synchronized (lock)
                            {

                                if (peerGroupConnections.get(newConnectionHandle) == null)
                                {
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
                                }
                                else
                                {
                                    System.err.println("Already connected to a peer with name: '" + newConnectionHandle + "'");
                                }
                            }
                        }
                    }
                    // Check for HELLO message with client name.

                }
                catch (IOException ex)
                {
                    Logger.getLogger(ChatNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }
    );

    private Message createDirMessage(String from, String to)
    {
        Message m = new Message(from, to);
        String content = "";
        for (Connection c : peerGroupConnections.values())
        {
            System.out.println(c.socket.toString().substring(13, 27));

            content += c.socket.toString().substring(13, 27) + ",";
        }
        m.append(content);
        return m;
    }

    public void begin() throws IOException
    {
        startPeerReceiver();
    }

    public String getHandle()
    {
        return handle;
    }

    public synchronized boolean hasPeerConnections()
    {
        return peerGroupConnections.size() > 0;
    }

    public synchronized List<String> getConnectionHandles()
    {
        List<String> peerGroupHandleList = new ArrayList<>();
        peerGroupConnections.
                values().
                stream().
                forEach(
                        (connection) ->
                {
                    peerGroupHandleList.add(connection.getHandle());
                }
                );

        Collections.sort(peerGroupHandleList);

        return Collections.unmodifiableList(peerGroupHandleList);
    }

    protected void startPeerReceiver() throws UnknownHostException, IOException
    {
        if (serverSocket == null)
        {
            InetAddress bindAddress = InetAddress.getByName(this.receiveIp);
            serverSocket = new ServerSocket(this.receivePort, 0, bindAddress);
            acceptThread.start();
        }
    }

    protected void addConnection(final Connection connection)
    {
        synchronized (lock)
        {
            if (peerGroupConnections.containsKey(connection.getHandle()))
            {
                System.err.println("[" + connection.getHandle() + "] is already an established connection.");
                return;
            }
            peerGroupConnections.put(connection.getHandle(), connection);
        }
    }

    protected synchronized boolean isalreadyConnected(final String ipAddress)
    {
        for (Connection c : peerGroupConnections.values())
        {
            if (c.hasIpAddress(ipAddress))
            {
                return true;
            }
        }

        return false;
    }

    /*
     * @param peer The peer that the message is being sent to 
     * @param message The message to send to all peers
     */
    public void sendMessage(Message message)
    {
        synchronized (lock)
        {
            if (message.isBroadcast())
            {
                //
                // Not handling broadcast messages presently...
                //
            }
            else
            {
                final List<String> receivers = message.getTo();

                for (String receiver : receivers)
                {
                    //find the socket of the peer using their handle:
                    Connection peerConnection = peerGroupConnections.get(receiver);

                    if (peerConnection != null)
                    {
                        peerConnection.sendMessage(message);
                    }
                    else
                    {
                        System.err.println("'" + receiver + "' is an unknown peer");
                    }
                }
            }
        }
    }
    
    //Messages are sent as a client.
    //


    public String getAddresses()
    {
        String output = "";
        for (Connection c : peerGroupConnections.values())
        {
            System.out.println(c.socket.toString().substring(13, 27));

            output += c.socket.toString().substring(13, 27) + ",";
        }
        return output;
    }
    
}