package com.coolspy3.csmodloader.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;

import com.coolspy3.csmodloader.Config;
import com.coolspy3.csmodloader.mod.ModLoader;
import com.coolspy3.csmodloader.network.ServerInstance;
import com.coolspy3.csmodloader.util.ComponentTableCellRenderer;
import com.coolspy3.csmodloader.util.TableButtonListener;
import com.coolspy3.csmodloader.util.UneditableTableModel;
import com.coolspy3.csmodloader.util.Utils;

/**
 * A JPanel used to display the primary GUI containing the server selection list
 */
class MainGUI extends JPanel implements ActionListener
{

    private static final long serialVersionUID = -6132080675073969912L;

    private final JButton addServerButton;
    private final JButton stopServersButton;
    private final JButton restartServersButton;
    private final JButton modsButton;
    private final JTable table;
    private final DefaultTableModel tableModel;

    MainGUI()
    {
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new GridLayout(1, 3));

        top.add(addServerButton = new JButton("Add Server"));
        top.add(stopServersButton = new JButton("Stop All Servers"));
        top.add(restartServersButton = new JButton("Restart Servers"));

        add(top, BorderLayout.NORTH);
        add(modsButton = new JButton(ModLoader.getModList().size() + " Mod"
                + (ModLoader.getModList().size() == 1 ? "" : "s") + " Loaded"), BorderLayout.SOUTH);

        addServerButton.addActionListener(this);
        stopServersButton.addActionListener(this);
        restartServersButton.addActionListener(this);
        modsButton.addActionListener(this);

        add(new JScrollPane(table = new JTable(tableModel = new UneditableTableModel(new String[] {
                "Name", "Ip", "Local Port", "Connect", "Auto Start", "Edit", "Delete", "^", "v"}, 0)
        {
            @Override
            public Class<?> getColumnClass(int columnIndex)
            {
                return columnIndex > 2 ? AbstractButton.class : String.class;
            }
        }), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        table.setDefaultRenderer(AbstractButton.class, new ComponentTableCellRenderer());

        table.addMouseListener(new TableButtonListener(table));

        updateTable();
    }

    /**
     * Updates this GUI to reflect the current server list and {@link ServerInstance} status
     */
    public synchronized void updateTable()
    {
        // Clear the table
        while (tableModel.getRowCount() > 0)
            tableModel.removeRow(0);

        // Add all servers
        Config.getInstance().serverList.stream().map(Config.getInstance().servers::get)
                .filter(Objects::nonNull)
                .map(server -> new Object[] {server.name, server.ip,
                        Integer.toString(server.localPort),
                        serverButton(new JButton("Connect"), ButtonAction.CONNECT, server.id),
                        serverButton(new JToggleButton("Auto Start", server.autoStart),
                                ButtonAction.AUTO_START, server.id),
                        serverButton(new JButton("Edit"), ButtonAction.EDIT, server.id),
                        serverButton(new JButton("Delete"), ButtonAction.DELETE, server.id),
                        serverButton(new JButton("^"), ButtonAction.MVUP, server.id),
                        serverButton(new JButton("v"), ButtonAction.MVDWN, server.id)})
                .forEach(tableModel::addRow);

        // Disable move buttons on endpoints (can't move top server up or bottom server down)
        if (tableModel.getRowCount() > 0)
        {
            ((JButton) tableModel.getValueAt(0, 7)).setEnabled(false);
            ((JButton) tableModel.getValueAt(tableModel.getRowCount() - 1, 8)).setEnabled(false);
        }

        // Only one server can be auto-started on a given port
        {
            List<Integer> autoStartedPorts =
                    // For every row
                    IntStream.range(0, tableModel.getRowCount())
                            // Get the AutoStart button
                            .mapToObj(row -> tableModel.getValueAt(row, 4))
                            .filter(obj -> obj instanceof JToggleButton)
                            .map(obj -> (JToggleButton) obj)
                            // If the associated server should be auto started
                            .map(btn -> (String) btn.getClientProperty("serverId"))
                            .map(Config.getInstance().servers::get)
                            .filter(server -> server.autoStart)
                            // Add that server's local port to the list
                            .map(server -> server.localPort).collect(Collectors.toList());

            // For every row
            IntStream.range(0, tableModel.getRowCount())
                    // Get the AutoStart button
                    .mapToObj(row -> tableModel.getValueAt(row, 4))
                    .filter(obj -> obj instanceof JToggleButton).map(obj -> (JToggleButton) obj)
                    // If the associated server should not be auto started
                    .filter(button -> !button.getModel().isSelected())
                    // And another server is set to be auto started on the same port
                    .filter(btn -> autoStartedPorts.contains(Config.getInstance().servers
                            .get(btn.getClientProperty("serverId")).localPort))
                    // Disable the button
                    .forEach(btn -> btn.setEnabled(false));
        }

        // For every row
        IntStream.range(0, tableModel.getRowCount())
                // Get the Edit and Delete buttons
                .mapToObj(row -> IntStream.of(5, 6).mapToObj(col -> tableModel.getValueAt(row, col))
                        .toArray(Object[]::new))
                .flatMap(Arrays::stream).filter(obj -> obj instanceof JButton)
                .map(obj -> (JButton) obj)
                // If the associated server is running
                .filter(btn -> ServerInstance.isRunning((String) btn.getClientProperty("serverId")))
                // Disable the buttons
                .forEach(btn -> btn.setEnabled(false));

        // For every row
        IntStream.range(0, tableModel.getRowCount())
                // Get the Connect button
                .mapToObj(row -> tableModel.getValueAt(row, 3))
                .filter(obj -> obj instanceof JButton).map(obj -> (JButton) obj)
                // If the associated server is not running
                .filter(btn -> !ServerInstance
                        .isRunning((String) btn.getClientProperty("serverId")))
                // But another server is running on the required port
                .filter(btn -> ServerInstance.getInstanceOnPort(Config.getInstance().servers
                        .get((String) btn.getClientProperty("serverId")).localPort) != null)
                // Disable the button
                .forEach(btn -> btn.setEnabled(false));

        // For every row
        IntStream.range(0, tableModel.getRowCount())
                // Get the Connect button
                .mapToObj(row -> tableModel.getValueAt(row, 3))
                .filter(obj -> obj instanceof JButton).map(obj -> (JButton) obj)
                // If the associated server is running
                .filter(btn -> ServerInstance.isRunning((String) btn.getClientProperty("serverId")))
                // Change the text to "Disconnect"
                .forEach(btn -> btn.setText("Disconnect"));
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

            config.servers.put(id, new Server(id, "New Server", "0.0.0.0", 25565, false));
            config.serverList.add(id);

            Config.safeSave();

            updateTable();
        }
        else if (source == stopServersButton) ServerInstance.stop();
        else if (source == restartServersButton) ServerInstance.restartServers();
        else if (source == modsButton) MainWindow.updateContent(new ModsGUI());
        else if (source instanceof AbstractButton)
        {
            AbstractButton btn = (AbstractButton) source;

            Object uncastAction = btn.getClientProperty("action");

            if (uncastAction == null) return;

            ButtonAction action = (ButtonAction) uncastAction;
            String id = (String) btn.getClientProperty("serverId");

            switch (action)
            {
                case CONNECT:
                    if (ServerInstance.isRunning(id)) ServerInstance.stop(id);
                    else
                        ServerInstance.start(id);

                    break;

                case AUTO_START:
                    Config.getInstance().servers.get(id).autoStart = btn.getModel().isSelected();

                    Config.safeSave();

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

    /**
     * Assigns a button the required properties to reference a particular server entry, and adds
     * this GUI as an ActionListener
     *
     * @param button The button to configure
     * @param action The action to associate with the button. This will be set as the "action"
     *        property
     * @param serverId The serverId to associate with the button. This will be set as the "serverId"
     *        property
     *
     * @return The button
     *
     * @see JComponent#putClientProperty
     */
    private final AbstractButton serverButton(AbstractButton button, ButtonAction action,
            String serverId)
    {
        button.putClientProperty("action", action);
        button.putClientProperty("serverId", serverId);

        button.addActionListener(this);

        return button;
    }

    private static enum ButtonAction
    {
        /**
         * Instructs the {@link ServerInstance} to connect or disconnect from the associated server
         */
        CONNECT,
        /**
         * Instructs the {@link ServerInstance} to automatically start when the mod loader starts
         */
        AUTO_START,
        /**
         * Opens a {@link ServerEditGUI} to allow the user to edit the associated server
         */
        EDIT,
        /**
         * Deletes the associated server
         */
        DELETE,
        /**
         * Shifts the associated server up in the display
         */
        MVUP,
        /**
         * Shifts the associated server down in the display
         */
        MVDWN;
    }

}
