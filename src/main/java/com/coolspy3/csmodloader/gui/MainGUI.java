package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import com.coolspy3.csmodloader.mod.ModLoader;

class MainGUI extends JPanel implements ActionListener
{

    private final JButton addServerButton;
    private final JButton modsButton;

    MainGUI()
    {
        setLayout(new BorderLayout());

        add(addServerButton = new JButton("Add Server"), BorderLayout.NORTH);
        add(modsButton = new JButton(ModLoader.getModList().size() + " Mods Loaded"),
                BorderLayout.SOUTH);

        addServerButton.addActionListener(this);
        modsButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        if (source == addServerButton)
        {

        }
        else if (source == modsButton) MainWindow.updateContent(new ModsGUI());
    }

}
