package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.coolspy3.csmodloader.util.Utils;

/**
 * A simple frame which displays a message and optional exception to the user
 */
public class TextAreaFrame extends JFrame
{

    private static final long serialVersionUID = 3425495168432306211L;

    /**
     * @param message The message to display in the frame
     */
    public TextAreaFrame(String message)
    {
        super("CoolSpy3 Mod Loader");

        setSize(600, 350);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);

        add(new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        setVisible(true);
    }

    /**
     * A convenience method for including the stacktrace of an exception in the message.
     *
     * @param message The message to display in the frame
     * @param e The exception of which to include the stacktraces
     */
    public TextAreaFrame(String message, Exception e)
    {
        this(message + "\n\n" + Utils.getStackTrace(e));
    }

    /**
     * A convenience method for including the stacktrace of an exception in the message.
     *
     * @param e The exception of which to include the stacktraces
     */
    public TextAreaFrame(Exception e)
    {
        this("An unknown error occurred!", e);
    }

}
