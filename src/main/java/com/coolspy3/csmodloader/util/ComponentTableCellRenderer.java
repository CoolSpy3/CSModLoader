package com.coolspy3.csmodloader.util;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class ComponentTableCellRenderer extends DefaultTableCellRenderer
{

        private static final long serialVersionUID = -2094184439274183528L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column)
        {
                return value instanceof Component ? (Component) value
                                : super.getTableCellRendererComponent(table, value, isSelected,
                                                hasFocus, row, column);
        }

}
