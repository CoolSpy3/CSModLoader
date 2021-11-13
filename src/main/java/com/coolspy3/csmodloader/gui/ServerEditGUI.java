package com.coolspy3.csmodloader.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.coolspy3.csmodloader.Config;

class ServerEditGUI extends JPanel implements ActionListener
{

    private static final long serialVersionUID = 7715666029410537275L;

    private final Server server;
    private final JTextField serverName, serverIp;
    private final JButton saveButton, cancelButton;

    ServerEditGUI(String serverId)
    {
        setLayout(new GridLayout(3, 2));

        server = Config.getInstance().servers.get(serverId);

        add(new JLabel("Server Name:"));
        add(serverName = new JTextField(server.name, 80));

        add(new JLabel("Server Address:"));
        add(serverIp = new JTextField(server.ip, 80));

        add(saveButton = new JButton("Save"));
        add(cancelButton = new JButton("Cancel"));

        saveButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (source == saveButton)
        {
            server.name = serverName.getText();
            server.ip = serverIp.getText();

            Config.safeSave();
        }
        else if (source != cancelButton) return;

        MainWindow.updateContent(new MainGUI());
    }

}
