package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.coolspy3.csmodloader.Utils;


public class TextAreaFrame extends JFrame {

    private static final long serialVersionUID = 3425495168432306211L;

    public TextAreaFrame(String message) {
        super("CoolSpy3 Mod Loader");
        setSize(600, 350);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        add(new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        setVisible(true);
    }

    public TextAreaFrame(String message, Exception e) {
        this(message + "\n\n" + Utils.getStackTrace(e));
    }

    public TextAreaFrame(Exception e) {
        this("An unknown error occured!", e);
    }

}
