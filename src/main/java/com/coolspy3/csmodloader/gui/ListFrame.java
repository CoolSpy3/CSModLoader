package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.coolspy3.csmodloader.util.UneditableTableModel;

/**
 * A simple frame which displays a message and a table
 */
public class ListFrame extends JFrame
{

    private static final long serialVersionUID = 3425495168432306211L;

    /**
     * Creates and displays a new frame with the given information
     *
     * @param message The message to display
     * @param columnNames The names of the columns to display in the table
     * @param data The data to display in the table as an array of rows
     */
    public ListFrame(String message, String[] columnNames, String[][] data)
    {
        super("CoolSpy3 Mod Loader");

        setSize(600, 350);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);

        JTable table = new JTable(new UneditableTableModel(data, columnNames));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        add(new JLabel(message), BorderLayout.NORTH);
        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        setVisible(true);
    }

}
