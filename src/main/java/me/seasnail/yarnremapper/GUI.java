package me.seasnail.yarnremapper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

public class GUI extends JFrame {
    public JLabel inputLabel;
    public JTextField inputPath;
    public JButton inputBrowse;

    public JLabel outputLabel;
    public JTextField outputPath;
    public JButton outputBrowse;

    public JLabel minecraftVersionLabel;
    public JComboBox<String> minecraftVersionSelector;
    public JCheckBox minecraftVersionSnapshots;

    public JLabel yarnVersionLabel;
    public JComboBox<String> yarnVersionSelector;

    public JButton remapButton;

    public GUI() throws URISyntaxException, IOException, InterruptedException {
        setTitle("Yarn Remapper");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        // Icon
        Image image = Toolkit.getDefaultToolkit().getImage(Main.class.getClassLoader().getResource("logo.png"));

        if (System.getProperty("os.name").contains("Mac")) Taskbar.getTaskbar().setIconImage(image);
        else setIconImage(image);

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu themeMenu = new JMenu("Theme");

        for (Theme theme : Main.THEMES) themeMenu.add(theme.createMenuItem());

        menuBar.add(themeMenu);
        setJMenuBar(menuBar);

        // Input
        inputLabel = new JLabel("Input");
        inputLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        inputPath = new JTextField();
        inputPath.setDropTarget(new JarDropTarget(file -> {
            inputPath.setText(file.getAbsolutePath());
            outputPath.setText(file.getAbsolutePath().replace(".jar", "-remapped.jar"));
        }));

        inputBrowse = new JButton("Browse");
        inputBrowse.addActionListener(e -> {
            // Create filter
            ByteBuffer jarFilter = MemoryUtil.memASCII("*.jar");

            PointerBuffer filters = MemoryUtil.memAllocPointer(1);
            filters.put(jarFilter);
            filters.rewind();

            // Open dialog
            String path = !inputPath.getText().equals("") ? inputPath.getText() : System.getProperty("user.home") + "/Desktop";
            String result = TinyFileDialogs.tinyfd_openFileDialog("Select input jar", path, filters, "JARs", false);

            if (result != null) {
                if (result.endsWith(".jar")) {
                    inputPath.setText(result);
                    outputPath.setText(result.replace(".jar", "-remapped.jar"));
                } else {
                    TinyFileDialogs.tinyfd_messageBox("Error selecting input jar!", "Please select a valid JAR file.", "ok", "error", false);
                }
            }

            // Free filter
            MemoryUtil.memFree(filters);
            MemoryUtil.memFree(jarFilter);
        });

        // Output
        outputLabel = new JLabel("Output");
        outputLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        outputPath = new JTextField();
        outputPath.setDropTarget(new JarDropTarget(file -> outputPath.setText(file.getAbsolutePath())));

        outputBrowse = new JButton("Browse");
        outputBrowse.addActionListener(e -> {
            // Create filter
            ByteBuffer jarFilter = MemoryUtil.memASCII("*.jar");

            PointerBuffer filters = MemoryUtil.memAllocPointer(1);
            filters.put(jarFilter);
            filters.rewind();

            String path = !outputPath.getText().equals("") ? outputPath.getText() : System.getProperty("user.home") + "/Desktop";
            String result = TinyFileDialogs.tinyfd_saveFileDialog("Save output JAR", path, filters, "JARs");

            if (result != null) {
                if (result.endsWith(".jar")) outputPath.setText(result);
                else outputPath.setText(result + ".jar");
            }

            // Free filter
            MemoryUtil.memFree(filters);
            MemoryUtil.memFree(jarFilter);
        });

        // Version Selectors
        minecraftVersionLabel = new JLabel("Minecraft Version");
        minecraftVersionLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        minecraftVersionSelector = new JComboBox<>();

        minecraftVersionSnapshots = new JCheckBox("Snapshots");

        yarnVersionLabel = new JLabel("Yarn Build");
        yarnVersionLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        yarnVersionSelector = new JComboBox<>();

        MinecraftVersion.populate(minecraftVersionSelector, minecraftVersionSnapshots.isSelected(), yarnVersionSelector);

        minecraftVersionSelector.addActionListener(e -> {
            try {
                yarnVersionSelector.removeAllItems();
                YarnVersion.populate(yarnVersionSelector, (String) minecraftVersionSelector.getSelectedItem());
            } catch (URISyntaxException | IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        minecraftVersionSnapshots.addActionListener(e -> {
            try {
                minecraftVersionSelector.removeAllItems();
                MinecraftVersion.populate(minecraftVersionSelector, minecraftVersionSnapshots.isSelected(), yarnVersionSelector);
            } catch (URISyntaxException | IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        // Remap Button
        remapButton = new JButton("Remap");
        remapButton.addActionListener(e -> {
            try {
                Main.remap(
                    new File(inputPath.getText()),
                    new File(outputPath.getText()),
                    (String) minecraftVersionSelector.getSelectedItem(),
                    Integer.parseInt(((String) yarnVersionSelector.getSelectedItem()).substring(6)),
                    false
                );
            } catch (IOException | InterruptedException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });

        GroupLayout contentPaneLayout = new GroupLayout(getContentPane());
        getContentPane().setLayout(contentPaneLayout);

        contentPaneLayout.setHorizontalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(
                    contentPaneLayout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(contentPaneLayout.createParallelGroup()
                            .addComponent(inputLabel, 50, 50, 50)
                            .addComponent(outputLabel, 50, 50, 50))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(contentPaneLayout.createParallelGroup()
                            .addComponent(outputPath, 350, 350, 350)
                            .addComponent(inputPath, 350, 350, 350))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(contentPaneLayout.createParallelGroup()
                            .addComponent(inputBrowse)
                            .addComponent(outputBrowse))
                        .addGap(30, 30, 30)
                )
                .addGroup(
                    contentPaneLayout.createSequentialGroup()
                        .addGap(70, 70, 70)
                        .addGroup(contentPaneLayout.createParallelGroup()
                            .addComponent(minecraftVersionLabel, 150, 150, 150)
                            .addComponent(yarnVersionLabel, 150, 150, 150)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(contentPaneLayout.createParallelGroup()
                            .addComponent(minecraftVersionSelector, 130, 130, 130)
                            .addComponent(yarnVersionSelector, 130, 130, 130)
                            .addComponent(remapButton, 130, 130, 130)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(contentPaneLayout.createParallelGroup()
                            .addComponent(minecraftVersionSnapshots)
                        )
                )
        );

        contentPaneLayout.setVerticalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addGap(20, 20, 20)
                    .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(inputLabel)
                        .addComponent(inputPath)
                        .addComponent(inputBrowse))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(outputLabel)
                        .addComponent(outputPath)
                        .addComponent(outputBrowse))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(minecraftVersionLabel)
                        .addComponent(minecraftVersionSelector)
                        .addComponent(minecraftVersionSnapshots))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(yarnVersionSelector)
                        .addComponent(yarnVersionLabel))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(remapButton, 50, 50, 50)
                    .addContainerGap(20, 20)
                )
        );

        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    private JMenuItem createThemeMenuItem(String name, Runnable runnable) {
        JMenuItem item = new JMenuItem(name);

        item.addActionListener(e -> {
            runnable.run();
            SwingUtilities.updateComponentTreeUI(this);
        });

        return item;
    }

    public static class MinecraftVersion {
        public String version;
        public boolean stable;

        public static void populate(JComboBox<String> mcSelector, boolean snapshots, JComboBox<String> yarnSelector) throws URISyntaxException, IOException, InterruptedException {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(new URI("https://meta.fabricmc.net/v2/versions/game/"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json");

            InputStream res = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofInputStream()).body();

            List<MinecraftVersion> versions = new Gson().fromJson(
                new InputStreamReader(res),
                new TypeToken<List<MinecraftVersion>>() {
                }.getType()
            );

            for (MinecraftVersion version : versions) {
                if (version.stable || snapshots) mcSelector.addItem(version.version);
            }

            yarnSelector.removeAllItems();
            YarnVersion.populate(yarnSelector, (String) mcSelector.getSelectedItem());
        }
    }

    public static class YarnVersion {
        public int build;

        public static void populate(JComboBox<String> dropDown, String mcVersion) throws URISyntaxException, IOException, InterruptedException {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(new URI("https://meta.fabricmc.net/v2/versions/yarn/" + mcVersion))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json");

            InputStream res = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofInputStream()).body();

            List<YarnVersion> versions = new Gson().fromJson(
                new InputStreamReader(res),
                new TypeToken<List<YarnVersion>>() {
                }.getType()
            );

            for (YarnVersion version : versions) dropDown.addItem("Build " + version.build);
        }
    }

    public static class JarDropTarget extends DropTarget {
        private final Consumer<File> action;

        public JarDropTarget(Consumer<File> action) {
            this.action = action;
        }

        @Override
        public synchronized void drop(DropTargetDropEvent event) {
            event.acceptDrop(DnDConstants.ACTION_REFERENCE);
            java.util.List<File> files = null;
            try {
                files = (List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
            }

            if (files == null || files.size() != 1) {
                TinyFileDialogs.tinyfd_messageBox("Error selecting input JAR!", "Please drop a valid JAR file.", "ok", "error", false);
                return;
            }

            File file = files.get(0);

            if (file.getName().endsWith(".jar")) {
                action.accept(file);
            }
            else {
                TinyFileDialogs.tinyfd_messageBox("Error selecting input JAR!", "Please drop a valid JAR file.", "ok", "error", false);
            }
        }
    }
}
