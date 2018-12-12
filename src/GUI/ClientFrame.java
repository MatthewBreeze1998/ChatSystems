/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import middleware.Agent;
import middleware.Message;
import middleware.MessageType;

/**
 *
 * @author s6089488
 */
public final class ClientFrame extends MyFrame
{

    Agent agent = new Agent("", this);
    final Insets INSETS_DATA = new Insets(2, 2, 2, 2);

    public ClientFrame()
    {
        super("Client");
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        setBackground(Color.yellow);
        setSize(450, 300);
        setResizable(false);

        try
        {
            String myHandle = getHandle();
            agent.setHandle(myHandle);
            agent.begin();
        }
        catch (IOException ex)
        {
            Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        addButtons();
        
        // sets the frame to be visible
        setVisible(true);
    }

    
   private void addButtons()
   {
       JLabel agentOptions = new JLabel("Client Options ", SwingConstants.CENTER);
        addComponentToGridBag(this, agentOptions, 0, 0, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);

        JButton agentNewConnections = new JButton("Connect to Portal");
        addComponentToGridBag(this, agentNewConnections, 0, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        agentNewConnections.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String ip = getIpAddress();
                connectTo(ip);
            }
        });

        JButton agentSendMessage = new JButton("Send Message");
        addComponentToGridBag(this, agentSendMessage, 0, 2, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        agentSendMessage.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String to = getTo();
                String content = getContent(to);
                sendMessage(to, content);
            }
        });

        JButton agentShowPortal = new JButton("Show Portal");
        addComponentToGridBag(this, agentShowPortal, 0, 3, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        agentShowPortal.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {

                displayConnections(agent);

            }
        });

        JButton agentRemoveConnections = new JButton("Remove Connections");
        addComponentToGridBag(this, agentRemoveConnections, 0, 4, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        agentRemoveConnections.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {

                agent.removeConnections();
            }
        });

        JButton agentexit = new JButton("Exit");
        addComponentToGridBag(this, agentexit, 0, 5, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        agentexit.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        });
   }

    
    
    @Override
    public void connectTo(String ip)
    {
        agent.connectTo(ip);
    }

    @Override
    public void sendMessage(String to, String content)
    {
        Message newMessage;

        if (to.equals("all"))
        {
            newMessage = new Message(agent.getHandle(), to, MessageType.BROADCAST);
        }
        else
        {
            newMessage = new Message(agent.getHandle(), to, MessageType.STANDARD);
        }

        newMessage.append(content);
        agent.sendMessage(newMessage);
    }

    protected String getTo()
    {
        Object[] msgOptions =
        {
            "Standard", "Broadcast"
        };

        int n = JOptionPane.showOptionDialog(null,
                "What message type are you sending?",
                "Send Message",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null,
                msgOptions, msgOptions[0]);

        String handle = "";

        if (n == 0)
        {
            //System.out.println("Current connections:");
            List<String> contacts = new ArrayList();
            for (String c : agent.getContacts())
            {
                contacts.add(c);
            }

            while (handle.isEmpty())
            {
                handle = JOptionPane.showInputDialog(null, "Current Contacts\n" + contacts + "\n\nWho would you like to message?", "Send Message");
                if (!handle.matches("^[^\\d\\s]+$") || handle.equals("Handle"))
                {
                    handle = "";
                }
            }
        }
        else
        {
            handle = "all";
        }

        return handle;
    }

    private void displayConnections(Agent me)
    {
        if (me.getPortal() == null)
        {
            JOptionPane.showMessageDialog(null, "No Agents Connected", "Connections", JOptionPane.ERROR_MESSAGE);
            return;
        }
        else
        {
            String connection = me.getPortal();
            JOptionPane.showMessageDialog(null, connection, "Connections", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
