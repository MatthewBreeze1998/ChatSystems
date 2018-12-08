/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peertopeer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.Pair;

/**
 *
 * @author v8269590
 */
public class Agent extends ChatNode {
    
    Pair<String, Connection> portal;
    
    public Agent(String handle) {
        super(handle);
    }
    
    public Agent(String handle, String receiveIp)
    {
        super(handle, receiveIp);
    }

    public Agent(String handle, String receiveIp, int receivePort)
    {
        super(handle, receiveIp, receivePort);
    }
    
    /*
     * @param peer The peer that the message is being sent to 
     * @param message The message to send to all peers
     */
    @Override
    public void sendMessage(Message message)
    {
        System.out.println("---Agent is sending message");
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
                System.out.println("---Message has a set receiver");
                if(portal != null)
                {
                    System.out.println("---Portal: " + portal.getKey() + " ... " + portal.getValue() + " is handling message");
                    portal.getValue().sendMessage(message);
                }
                else
                {
                    System.out.println("Portal is null...");
                }
            }
        }
    }
    
    @Override
    public void connectTo(final String remoteIpAddress, final int remotePort)
    {
        // check if we're already connected, perhaps the remote device
        // instigated a connection previously.
        if (isalreadyConnected(remoteIpAddress))
        {
            //System.err.println(String.format("Already connected to the peer with IP: '%s'", remoteIpAddress));
            return;
        }

        //Create a thread to instigate the HELLO handshake between this peer
        //and the remote peer
        Thread helloAgentThread = new Thread(
                new Runnable()
        {
            @Override
            public void run()
            {
                InetAddress bindAddress;
                try
                {
                    bindAddress = InetAddress.getByName(remoteIpAddress);
                    Socket newSocket = new Socket(bindAddress, remotePort);
                    Connection partialConnection = new Connection(newSocket);
                    partialConnection.sendMessage(Message.createAgentMessage(handle));

                    //Wait for a response from this connection.
                    while (!partialConnection.hasMessage())
                    {
                        // ... Do nothing ...
                        // assumes it will eventually connect... probably not a good idea...
                    }

                    //We should have a HELLOACK message, which will have
                    //the handle of the remote peer
                    final Message receivedMessage = partialConnection.receiveMessage();
                    //Message ackMessage = partialConnection.receiveMessage();

                    if (receivedMessage.isHelloAckMessage())
                    {
                        partialConnection.setHandle(receivedMessage.getFrom());
                        addConnection(partialConnection);
                    }
                }
                catch (UnknownHostException ex)
                {
                    Logger.getLogger(ChatNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                catch (IOException ex)
                {
                    Logger.getLogger(ChatNode.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
        );

        helloAgentThread.start();

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
                                    newConnection.sendMessage(Message.createHelloAckMessage(handle, newConnectionHandle));
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
    
    @Override
    protected void addConnection(final Connection connection)
    {
        synchronized (lock)
        {
            portal = new Pair<>(connection.getHandle(), connection);
            System.out.println("---Connected to portal " + portal.getKey() + " ... " + portal.getValue());
        }
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
    
    public String getPortal()
    {
        return portal.getKey();
    }

    @Override
    public void removeConnections()
    {
        portal = null;
    }

    @Override
    public void begin() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
