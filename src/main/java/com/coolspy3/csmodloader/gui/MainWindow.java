package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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

        setContent(new MainGUI());

        setVisible(true);
    }

    void setContent(JPanel newContent)
    {
        if (content != null) remove(content);
        content = newContent;
        add(content, BorderLayout.CENTER);
        revalidate();
    }

    static MainWindow get()
    {
        return window;
    }

    static void updateContent(JPanel newContent)
    {
        get().setContent(newContent);
    }

    public static void create()
    {
        if (window == null) SwingUtilities.invokeLater(() -> window = new MainWindow());
    }

}
