package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.coolspy3.csmodloader.mod.Mod;
import com.coolspy3.csmodloader.mod.ModLoader;
import com.coolspy3.csmodloader.util.UneditableTableModel;

/**
 * A JPanel used to display a list of the mods which are currently loaded
 */
class ModsGUI extends JPanel implements ActionListener
{

    private static final long serialVersionUID = 8508975564412920622L;

    private final JButton backButton;

    ModsGUI()
    {
        setLayout(new BorderLayout());

        add(backButton = new JButton("Back"), BorderLayout.NORTH);

        add(new JScrollPane(new JTable(new UneditableTableModel(

                ModLoader.getModList().stream()
                        .sorted(Comparator.comparing(Mod::id, String.CASE_INSENSITIVE_ORDER))
                        .map(mod -> new String[] {mod.name(), mod.id(), mod.version(),
                                mod.description(),
                                Arrays.stream(mod.dependencies())
                                        .collect(Collectors.joining(", "))})
                        .toArray(String[][]::new),

                new String[] {"Name", "Id", "Version", "Description", "Dependencies"})),

                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        backButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == backButton) MainWindow.updateContent(new MainGUI());
    }

}
