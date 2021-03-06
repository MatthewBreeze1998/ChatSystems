/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package middleware;

import java.util.LinkedList;

/**
 * Used as an endpoint in the middleware for message sending and receiving
 * @author Group B
 */
public class Agent extends Node
{
    /**
     * The portal that the agent is connected to
     */
    private Portal portal;

    /**
     * the client that is using the agent
     */
    private Contactable client;

    /**
     * A list of the handles of agents that have previously contacted this
     * portal
     */
    private LinkedList<String> contacts = new LinkedList<>();

    /**
     * A default constructor, essentially makes a null Agent
     */
    public Agent()
    {
        handle = null;
        portal = null;
    }

    /**
     * The standard constructor, gives the agent a handle and a portal to
     * connect to
     *
     * @param handle the identifying handle of the agent
     * @param portal the portal that the agent is connected to
     */
    public Agent(String handle, Portal portal)
    {
        this.handle = handle;
        this.portal = portal;
        portal.addAgent(this);
    }

    /**
     * sends a standard message from this agent to another
     *
     * @param to the handle of the agent to send a message to
     * @param content the content of the message
     */
    public void sendMessage(String to, String content)
    {
        Message m = new Message(handle, to, MessageType.STANDARD);
        m.append(content);

        portal.enqueue(m);
        portal.sendMessage();
    }

    /**
     * sends a standard message from this agent to another
     *
     * @param content the content of the message
     */
    public void sendBroadcast(String content)
    {
        Message m = new Message(handle, null, MessageType.BROADCAST);
        m.append(content);

        portal.enqueue(m);
        portal.sendMessage();
    }

    /**
     * This agent handles a message, then adds the sender to a contact list
     *
     * @param m the message to receive
     */
    public void receiveMessage(Message m)
    {
        if (nodeMonitor != null)
        {
            nodeMonitor.handleMessage(m);
        }

        if (client != null)
        {
            client.handleMessage(m);
        }

        if (!contacts.contains(m.getFrom()))
        {
            contacts.add(m.getFrom());
        }
    }

    /**
     * Sets the client for this agent
     *
     * @param c the client to connect to this agent
     */
    public void setClient(Contactable c)
    {
        client = c;
    }

    /**
     * gets the contact list
     *
     * @return the list of all agents that have contacted this portal
     */
    public LinkedList<String> getContacts()
    {
        return contacts;
    }

    /**
     * Gets the portal this agent is connected to
     *
     * @return the portal that the agent is connected to
     */
    public Portal getPortal()
    {
        return portal;
    }

    /**
     * Removes this agent from the portal, then sets all values to null
     */
    public void delete()
    {
        portal.removeAgent(handle);
        this.handle = null;
        this.client = null;
        this.contacts = null;
        this.nodeMonitor = null;
        this.portal = null;
    }
}
