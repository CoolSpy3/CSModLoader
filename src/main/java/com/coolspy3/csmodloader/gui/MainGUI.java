package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import com.coolspy3.csmodloader.Config;
import com.coolspy3.csmodloader.mod.ModLoader;
import com.coolspy3.csmodloader.network.ServerInstance;
import com.coolspy3.csmodloader.util.ComponentTableCellRenderer;
import com.coolspy3.csmodloader.util.UneditableTableModel;
import com.coolspy3.csmodloader.util.Utils;

class MainGUI extends JPanel implements ActionListener
{

    private static final long serialVersionUID = -6132080675073969912L;

    private final JButton addServerButton;
    private final JButton modsButton;
    private final JTable table;
    private final DefaultTableModel tableModel;

    MainGUI()
    {
        setLayout(new BorderLayout());

        add(addServerButton = new JButton("Add Server"), BorderLayout.NORTH);
        add(modsButton = new JButton(ModLoader.getModList().size() + " Mods Loaded"),
                BorderLayout.SOUTH);

        addServerButton.addActionListener(this);
        modsButton.addActionListener(this);

        add(new JScrollPane(table = new JTable(tableModel = new UneditableTableModel(
                new String[] {"Name", "Ip", "Connect", "Edit", "Delete", "^", "v"}, 0)
        {
            @Override
            public Class<?> getColumnClass(int columnIndex)
            {
                return columnIndex > 1 ? JButton.class : String.class;
            }
        }), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        table.setDefaultRenderer(JButton.class, new ComponentTableCellRenderer());

        updateTable();
    }

    public void updateTable()
    {
        while (tableModel.getRowCount() > 0)
            tableModel.removeRow(0);

        Config.getInstance().serverList.stream().map(Config.getInstance().servers::get)
                .filter(Objects::nonNull)
                .map(server -> new Object[] {server.name, server.ip,
                        serverButton("Connect", ButtonAction.CONNECT, server.id),
                        serverButton("Edit", ButtonAction.EDIT, server.id),
                        serverButton("Delete", ButtonAction.DELETE, server.id),
                        serverButton("^", ButtonAction.MVUP, server.id),
                        serverButton("v", ButtonAction.MVDWN, server.id)})
                .forEach(tableModel::addRow);

        if (ServerInstance.isRunning())
        {
            IntStream.range(0, tableModel.getRowCount())
                    .mapToObj(row -> tableModel.getValueAt(row, 2))
                    .filter(obj -> obj instanceof JButton).map(obj -> (JButton) obj)
                    .forEach(btn -> btn.setEnabled(false));

            IntStream.range(0, tableModel.getRowCount())
                    .mapToObj(row -> tableModel.getValueAt(row, 2))
                    .filter(obj -> obj instanceof JButton).map(obj -> (JButton) obj)
                    .filter(btn -> btn.getClientProperty("serverId") == ServerInstance
                            .getRunningServerId())
                    .forEach(btn -> {
                        btn.setText("Disconnect");
                        btn.setEnabled(true);
                    });
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (source == addServerButton)
        {
            Config config = Config.getInstance();

            String id;

            do
                id = Utils.randomString(32);
            while (config.serverList.contains(id));

            config.servers.put(id, new Server(id, "New Server", "0.0.0.0"));
            config.serverList.add(id);

            Config.safeSave();

            updateTable();
        }
        else if (source == modsButton) MainWindow.updateContent(new ModsGUI());
        else if (source instanceof JButton)
        {
            JButton btn = (JButton) source;

            Object uncastAction = btn.getClientProperty("action");

            if (uncastAction == null) return;

            ButtonAction action = (ButtonAction) uncastAction;
            String id = (String) btn.getClientProperty("serverId");

            switch (action)
            {
                case CONNECT:
                    if (ServerInstance.isRunning())
                    {
                        if (((JButton) tableModel
                                .getValueAt(Config.getInstance().serverList.indexOf(id), 2))
                                        .isEnabled())
                            ServerInstance.stop();

                        break;
                    }

                    ServerInstance.start(Config.getInstance().servers.get(id).ip);

                    break;

                case EDIT:
                    MainWindow.updateContent(new ServerEditGUI(id));

                    break;

                case DELETE:
                    Config config = Config.getInstance();

                    config.serverList.remove(id);
                    config.servers.remove(id);

                    Config.safeSave();

                    break;

                case MVUP:
                    Config.getInstance().serverList.shiftUp(id);

                    Config.safeSave();

                    break;

                case MVDWN:
                    Config.getInstance().serverList.shiftDown(id);

                    Config.safeSave();

                    break;
            }
        }

        updateTable();
    }

    private final JButton serverButton(String text, ButtonAction action, String serverId)
    {
        JButton button = new JButton(text);

        button.putClientProperty("action", action);
        button.putClientProperty("serverId", serverId);

        button.addActionListener(this);

        return button;
    }

    private static enum ButtonAction
    {
        CONNECT, EDIT, DELETE, MVUP, MVDWN;
    }

}
