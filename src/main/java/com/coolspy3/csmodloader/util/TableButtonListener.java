package com.coolspy3.csmodloader.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.JTable;

/**
 * A MouseListener which passes on mouse button clicks to {@link AbstractButton}s in a
 * {@link JTable}.
 */
public class TableButtonListener extends MouseAdapter
{

    private final JTable table;

    public TableButtonListener(JTable table)
    {
        this.table = table;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        int column = table.getColumnModel().getColumnIndexAtX(e.getX());
        int row = e.getY() / table.getRowHeight();
        if (row < table.getRowCount() && row >= 0 && column < table.getColumnCount() && column >= 0)
        {
            Object value = table.getValueAt(row, column);
            if (value instanceof AbstractButton)
            {
                ((AbstractButton) value).doClick();
            }
        }
    }
}
