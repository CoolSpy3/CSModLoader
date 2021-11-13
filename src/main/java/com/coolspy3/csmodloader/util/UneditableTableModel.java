package com.coolspy3.csmodloader.util;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

public class UneditableTableModel extends DefaultTableModel
{

    private static final long serialVersionUID = 3279251307637537531L;

    public UneditableTableModel()
    {}

    public UneditableTableModel(int rowCount, int columnCount)
    {
        super(rowCount, columnCount);
    }

    public UneditableTableModel(Vector<?> columnNames, int rowCount)
    {
        super(columnNames, rowCount);
    }

    public UneditableTableModel(Object[] columnNames, int rowCount)
    {
        super(columnNames, rowCount);
    }

    public UneditableTableModel(Vector<?> data, Vector<?> columnNames)
    {
        super(data, columnNames);
    }

    public UneditableTableModel(Object[][] data, Object[] columnNames)
    {
        super(data, columnNames);
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
        return false;
    }
}
