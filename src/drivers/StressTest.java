/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drivers;

import external.Client;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import middleware.Portal;

/**
 * Tests the infrastructure with a high Client count
 *
 * @author Group B
 */
public class StressTest {

    /**
     * The portal used on this pc for clients to connect to
     */
    static Portal portal;

    /**
     * The list of clients created on this pc
     */
    static LinkedList<Client> clients = new LinkedList<Client>();

    /**
     * The main driver of the class
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        portal = new Portal("portal", "0.0.0.0");
        portal.setDelay(0);
        
        try 
        {
            portal.begin();

            NormalStressTest(1000);
            //BroadcastStressTest(100);
            //ExternalStressTest(100, "cam", "152.105.67.111");
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(Portal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Sends messages from one client to another for every created client
     * @param clientCount the amount of clients to be created. Also the amount of messages to be sent
     */
    public static void NormalStressTest(int clientCount) 
    {
        for (int i = 0; i < clientCount; i++) 
        {
            clients.add(new Client("client" + i, portal));
        }

        System.out.println("clients made");

        for (int i = 0; i < clients.size(); i++) 
        {
            Client sender = clients.get(i);
            String receiver = clients.get(clients.size() - i - 1).getName();
            sender.sendMessage(receiver, "message " + i);
        }
    }

    /**
     * Sends a broadcast message from every client
     * @param clientCount the amount of clients to be created. 
     *                    will send clientCount^2 messages
     */
    public static void BroadcastStressTest(int clientCount) 
    {
        for (int i = 0; i < clientCount; i++) 
        {
            clients.add(new Client("client" + i, portal));
        }

        System.out.println("clients made");

        for (int i = 0; i < clients.size(); i++) 
        {
            Client sender = clients.get(i);
            sender.sendBroadcast("message " + i);
        }
    }

    /**
     * Creates clients to send messages to an external client
     * @param clientCount the amount of clients to be created
     * @param receiver the handle of the receiver
     * @param connectTo the ip to connect to
     */
    public static void ExternalStressTest(int clientCount, String receiver, String connectTo) 
    {
        portal.connectTo(connectTo);
        System.out.println("connected");
        
        for(String s : portal.getPortalHandles())
        {
            System.out.println(s);
        }
        
        for (int i = 0; i < clientCount; i++) 
        {
            clients.add(new Client("client" + i, portal));
        }
        System.out.println("clients made");

        for (int i = 0; i < clients.size(); i++) 
        {
            Client sender = clients.get(i);
            System.out.println("Sending Message " + i);
            sender.sendMessage(receiver, "message " + i);
            System.out.println("size: " + portal.getQueueSize());
        }
    }
}
