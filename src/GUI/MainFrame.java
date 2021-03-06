/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The main frame that has options to open any other frame
 * @author Group B
 */
public final class MainFrame extends BaseFrame
{

    /**
     * Initialising empty middleware nodes
     */
    DirectoryFrame directoryFrame;
    PortalFrame portalFrame;
    ClientFrame clientFrame;

    /**
     * Constructs a swing frame Populates the frame with buttons
     */
    public MainFrame()
    {
        super("Main");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        this.setSize(300, 200);

        addButtons();
        this.setVisible(true);
    }

    /**
     * Initialises swing buttons Adds them to the frame grid bag. Defines action
     * listeners for each button.
     */
    @Override
    protected void addButtons()
    {
        JLabel choice = new JLabel("Portal, Client, or Directory ? ", SwingConstants.CENTER);
        addComponentToGridBag(this, choice, 0, 0, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);

        JButton Portal = new JButton("Portal");
        addComponentToGridBag(this, Portal, 0, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        Portal.addActionListener((ActionEvent e) ->
        {
            if (portal == null) 
            {
              portalFrame = new PortalFrame();              
            }
            else
            {
                JOptionPane.showMessageDialog(null, "A Portal already exists.");
            }
            
        });

        JButton agents = new JButton("Client");
        addComponentToGridBag(this, agents, 0, 2, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        agents.addActionListener((ActionEvent e) ->
        {
            if (portal == null) 
            {
               JOptionPane.showMessageDialog(null, "A Portal is required first.");
            }
            else
            {
                clientFrame = new ClientFrame();
            }
        });

        JButton Directory = new JButton("Directory");
        addComponentToGridBag(this, Directory, 0, 3, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        Directory.addActionListener((ActionEvent e) ->
        {
            directoryFrame = new DirectoryFrame();
        });

        JButton exit = new JButton("Exit");
        addComponentToGridBag(this, exit, 0, 4, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        exit.addActionListener((ActionEvent e) ->
        {
            // close first Frame
            this.dispose();
            portalFrame.dispose();
            clientFrame.dispose();
            directoryFrame.dispose();
        });
    }
    
}
