package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;

class ModsGUI extends JPanel implements ActionListener
{

    private final JButton backButton;

    ModsGUI()
    {
        setLayout(new BorderLayout());

        add(backButton = new JButton("Back"), BorderLayout.NORTH);

        backButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == backButton) MainWindow.updateContent(new MainGUI());
    }

}
