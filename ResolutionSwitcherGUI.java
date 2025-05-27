import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.awt.FlowLayout;
import java.lang.reflect.Type;

public class ResolutionSwitcherGUI extends JFrame {
    private static final String RESOLUTIONS_FILE = "resolutions.json";
    private static final String QRES_PATH = "C:\\Tools\\QRes\\QRes.exe";

    private List<Resolution> resolutions = new ArrayList<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Width", "Height", "Refresh Rate"}, 0) {
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final Gson gson = new Gson();

    public ResolutionSwitcherGUI() {
        setTitle("Resolution Switcher");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        loadResolutions();
        refreshTable();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton removeBtn = new JButton("Remove");
        JButton applyBtn = new JButton("Apply");

        addBtn.addActionListener(e -> addResolution());
        editBtn.addActionListener(e -> editResolution());
        removeBtn.addActionListener(e -> removeResolution());
        applyBtn.addActionListener(e -> applySelectedResolution());

        for (JButton btn : new JButton[]{addBtn, editBtn, removeBtn, applyBtn}) {
            buttonPanel.add(btn);
        }

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Resolution res : resolutions) {
            tableModel.addRow(new Object[]{res.getWidth(), res.getHeight(), res.getRefreshRate()});
        }
    }

    private void loadResolutions() {
        File file = new File(RESOLUTIONS_FILE);
        if (!file.exists()) {
            resolutions.add(new Resolution(1920, 1080, 60));
            resolutions.add(new Resolution(1280, 720, 60));
            saveResolutions();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            java.lang.reflect.Type listType = new TypeToken<List<Resolution>>() {}.getType();
            resolutions = gson.fromJson(reader, listType);
            if (resolutions == null) resolutions = new ArrayList<>();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to load resolutions.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveResolutions() {
        try (Writer writer = new FileWriter(RESOLUTIONS_FILE)) {
            gson.toJson(resolutions, writer);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save resolutions.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addResolution() {
        ResolutionDialog dialog = new ResolutionDialog(this, null);
        dialog.setVisible(true);
        Resolution res = dialog.getResolution();
        if (res != null) {
            resolutions.add(res);
            saveResolutions();
            refreshTable();
        }
    }

    private void editResolution() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a resolution to edit.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Resolution existing = resolutions.get(row);
        ResolutionDialog dialog = new ResolutionDialog(this, existing);
        dialog.setVisible(true);
        Resolution updated = dialog.getResolution();
        if (updated != null) {
            resolutions.set(row, updated);
            saveResolutions();
            refreshTable();
        }
    }

    private void removeResolution() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a resolution to remove.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Remove selected resolution?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            resolutions.remove(row);
            saveResolutions();
            refreshTable();
        }
    }

    private void applySelectedResolution() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a resolution to apply.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Resolution res = resolutions.get(row);
        try {
            ProcessBuilder pb = new ProcessBuilder(QRES_PATH, "/x:" + res.getWidth(), "/y:" + res.getHeight(), "/r:" + res.getRefreshRate());
            pb.inheritIO();
            Process proc = pb.start();
            int exitCode = proc.waitFor();
            if (exitCode != 0) throw new IOException("QRes failed with exit code: " + exitCode);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to apply resolution.\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf. Using default.");
        }

        SwingUtilities.invokeLater(() -> {
            ResolutionSwitcherGUI gui = new ResolutionSwitcherGUI();
            gui.setVisible(true);
        });
    }

    // ==== Supporting Classes ====

    public static class Resolution {
        private int width;
        private int height;
        private int refreshRate;

        public Resolution(int width, int height, int refreshRate) {
            this.width = width;
            this.height = height;
            this.refreshRate = refreshRate;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getRefreshRate() { return refreshRate; }

        public void setWidth(int width) { this.width = width; }
        public void setHeight(int height) { this.height = height; }
        public void setRefreshRate(int refreshRate) { this.refreshRate = refreshRate; }
    }

    public static class ResolutionDialog extends JDialog {
        private final JTextField widthField = new JTextField();
        private final JTextField heightField = new JTextField();
        private final JTextField refreshField = new JTextField();
        private Resolution resolution = null;

        public ResolutionDialog(Frame parent, Resolution existing) {
            super(parent, true);
            setTitle(existing == null ? "Add Resolution" : "Edit Resolution");
            setSize(300, 220);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));

            JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            inputPanel.add(new JLabel("Width:"));
            inputPanel.add(widthField);
            inputPanel.add(new JLabel("Height:"));
            inputPanel.add(heightField);
            inputPanel.add(new JLabel("Refresh Rate:"));
            inputPanel.add(refreshField);

            if (existing != null) {
                widthField.setText(String.valueOf(existing.getWidth()));
                heightField.setText(String.valueOf(existing.getHeight()));
                refreshField.setText(String.valueOf(existing.getRefreshRate()));
            }

            JPanel buttonPanel = new JPanel();
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            okButton.addActionListener(e -> {
                try {
                    int w = Integer.parseInt(widthField.getText());
                    int h = Integer.parseInt(heightField.getText());
                    int r = Integer.parseInt(refreshField.getText());
                    if (w <= 0 || h <= 0 || r <= 0) throw new NumberFormatException();
                    resolution = new Resolution(w, h, r);
                    dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Enter valid positive integers.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelButton.addActionListener(e -> dispose());

            add(inputPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        public Resolution getResolution() {
            return resolution;
        }
    }
}