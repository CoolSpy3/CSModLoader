package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;


public class ListFrame extends JFrame
{

    private static final long serialVersionUID = 3425495168432306211L;

    public ListFrame(String message, String[] columnNames, String[][] data)
    {
        super("CoolSpy3 Mod Loader");

        setSize(600, 350);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);

        JTable table = new JTable(new DefaultTableModel(data, columnNames)
        {

            private static final long serialVersionUID = 3279251307637537531L;

            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }

        });
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        add(new JLabel(message), BorderLayout.NORTH);
        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        setVisible(true);
    }

}
