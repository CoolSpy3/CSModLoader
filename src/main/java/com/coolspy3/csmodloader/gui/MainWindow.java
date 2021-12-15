package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.coolspy3.csmodloader.network.ServerInstance;
import com.coolspy3.csmodloader.util.Utils;

/**
 * Represents a JFrame with a changeable JPanel content to be used as the primary GUI of the program
 */
public class MainWindow extends JFrame
{

    private static final long serialVersionUID = 5717822378368714313L;

    private static MainWindow window;

    private JPanel content;

    private MainWindow()
    {
        super("CoolSpy3 Mod Loader");
        setSize(1000, 640);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                ServerInstance.stop();
            }
        });

        setContent(new MainGUI());

        setVisible(true);
    }

    /**
     * Updates the content displayed in this frame
     *
     * @param newContent The new content to display
     */
    void setContent(JPanel newContent)
    {
        if (content != null) remove(content);
        content = newContent;
        add(content, BorderLayout.CENTER);
        revalidate();
    }

    /**
     * @return The global instance of the window used by the program
     */
    static MainWindow get()
    {
        return window;
    }

    /**
     * Updates the content displayed on the global instance of the window. This is equivalent to
     * invoking {@code get().setContent(newContent)}
     *
     * @param newContent The new content to display
     */
    static void updateContent(JPanel newContent)
    {
        get().setContent(newContent);
    }

    /**
     * Creates and initializes a new frame and sets it as the global frame using
     * {@link SwingUtilities#invokeAndWait(Runnable)}.
     *
     * This method has no effect if the global window has already been set.
     */
    public static synchronized void create()
    {
        if (window == null)
            Utils.safe(() -> SwingUtilities.invokeAndWait(() -> window = new MainWindow()));
    }

}
