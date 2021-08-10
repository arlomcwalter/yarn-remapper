package me.seasnail.yarnremapper;

import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.apache.commons.io.FileUtils;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;

public class Main {
    public static final File CACHE_DIR = new File(System.getProperty("user.home"), ".yarn-remapper");
    public static GUI gui;

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        gui = new GUI();

        // Input
        gui.inputPath.setDropTarget(new JarDropTarget(gui, file -> gui.inputPath.setText(file.getAbsolutePath())));

        gui.inputBrowse.addActionListener(e -> {
            // Create filter
            ByteBuffer pngFilter = MemoryUtil.memASCII("*.jar");

            PointerBuffer filters = MemoryUtil.memAllocPointer(1);
            filters.put(pngFilter);
            filters.rewind();

            // Open dialog
            String result = TinyFileDialogs.tinyfd_openFileDialog("Select input jar", System.getProperty("user.home") + "/Desktop", filters, "JARs", false);

            if (result != null) {
                if (result.endsWith(".jar")) {
                    gui.inputPath.setText(result);
                } else {
                    TinyFileDialogs.tinyfd_messageBox("Error selecting input jar!", "Please select a valid JAR file.", "ok", "error", false);
                }
            }

            // Free filter
            MemoryUtil.memFree(filters);
            MemoryUtil.memFree(pngFilter);
        });

        // Output
        gui.outputPath.setDropTarget(new JarDropTarget(gui, file -> gui.outputPath.setText(file.getAbsolutePath())));

        gui.outputBrowse.addActionListener(e -> {
            String result = TinyFileDialogs.tinyfd_selectFolderDialog("Select output folder", System.getProperty("user.home") + "/Desktop");

            if (result != null) {
                gui.outputPath.setText(result);
            }
        });

        // Versions
        Utils.populateMcVers(gui.mcVersionSelector, gui.mcVersionSnapshots.isSelected(), gui.yarnVersionSelector);

        gui.mcVersionSelector.addActionListener(e -> {
            try {
                gui.yarnVersionSelector.removeAllItems();
                Utils.populateYarnVers(gui.yarnVersionSelector, (String) gui.mcVersionSelector.getSelectedItem());
            } catch (URISyntaxException | IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        gui.mcVersionSnapshots.addActionListener(e -> {
            try {
                gui.mcVersionSelector.removeAllItems();
                Utils.populateMcVers(gui.mcVersionSelector, gui.mcVersionSnapshots.isSelected(), gui.yarnVersionSelector);
            } catch (URISyntaxException | IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        // Run
        gui.remapButton.addActionListener(e -> {
            try {
                remap(
                    new File(gui.inputPath.getText()),
                    new File(gui.outputPath.getText()),
                    (String) gui.mcVersionSelector.getSelectedItem(),
                    parseBuild((String) gui.yarnVersionSelector.getSelectedItem()));
            } catch (IOException | InterruptedException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });

        gui.setVisible(true);
    }

    public static int parseBuild(String input) {
        return Integer.parseInt(input.substring(6));
    }

    public static void remap(File input, File output, String mcVersion, int yarnBuild) throws IOException, URISyntaxException, InterruptedException {
        if (!input.exists() || !input.getName().endsWith(".jar")) {
            JOptionPane.showMessageDialog(gui, "The input JAR could not be read, please check it is correct.", "Error reading input JAR!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!output.exists()) output.mkdirs();
        if (!output.exists() || !output.isDirectory()) {
            JOptionPane.showMessageDialog(gui, "The output directory could not be found.", "Invalid output directory!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!gui.progressBarVisible) gui.content.add(gui.progressPanel());

        File mcVersionFolder = new File(Main.CACHE_DIR, mcVersion);
        String yarnVersion = mcVersion + "+build." + yarnBuild;

        if (!mcVersionFolder.exists()) mcVersionFolder.mkdir();

        File mappingsFile = new File(mcVersionFolder, yarnBuild + ".tiny");
        if (!mappingsFile.exists()) {
            // Download yarn mappings
            gui.progressBar.setValue(0);
            gui.progressLabel.setText(String.format("Downloading Yarn mappings for Minecraft %s (Build %d)...", mcVersion, yarnBuild));

            File jarFile = new File(mcVersionFolder, "yarn.jar");
            if (jarFile.exists()) jarFile.delete();

            jarFile.deleteOnExit();

            try {
                FileUtils.copyURLToFile(new URL("https://maven.fabricmc.net/net/fabricmc/yarn/" + yarnVersion + "/yarn-" + yarnVersion + "-v2.jar"), jarFile);
                gui.progressLabel.setText("Complete");
                gui.progressBar.setValue(100);
            } catch (IOException e) {
                gui.progressLabel.setText("Failed.");
                gui.progressBar.setValue(50);
                System.exit(0);
                return;
            }

            // Extract mappings from jar
            gui.progressBar.setValue(0);
            gui.progressLabel.setText("Extracting tiny mappings from Yarn jar...");

            try (FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
                Files.copy(jar.getPath("mappings/mappings.tiny"), mappingsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                gui.progressLabel.setText("Complete.");
                gui.progressBar.setValue(100);
            } catch (IOException e) {
                gui.progressLabel.setText("Failed");
                gui.progressBar.setValue(50);
                System.exit(0);
                return;
            }

            jarFile.delete();
        }

        if (!mappingsFile.exists()) {
            JOptionPane.showMessageDialog(gui, "Please check the versions you have selected are correct.", "Error fetching mappings!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        gui.progressBar.setValue(0);
        gui.progressLabel.setText(String.format("Remapping %s with %s (Build %d) mappings...", input.getName(), mcVersion, yarnBuild));

        TinyRemapper remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappingsFile.toPath(), "intermediary", "named"))
            .build();

        File outputFile = new File(output, input.getName().replace(".jar", "-remap.jar"));

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(outputFile.toPath()).build()) {
            outputConsumer.addNonClassFiles(input.toPath(), NonClassCopyMode.FIX_META_INF, remapper);

            remapper.readInputs(input.toPath());
            remapper.apply(outputConsumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            remapper.finish();
            gui.progressBar.setValue(100);
            gui.progressLabel.setText("Complete");
        }
    }

    public static class MinecraftVersion {
        public String version;
        public boolean stable;
    }

    public static class YarnVersion {
        public int build;
    }

    public static class JarDropTarget extends DropTarget {
        private final Component parent;
        private final Consumer<File> action;

        public JarDropTarget(Component parent, Consumer<File> action) {
            this.parent = parent;
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
                JOptionPane.showMessageDialog(parent, "Please drop a valid JAR file.", "Error selecting input file!", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File file = files.get(0);

            if (file.getName().endsWith(".jar")) action.accept(file);
            else {
                JOptionPane.showMessageDialog(parent, "Please drop a valid JAR file.", "Error selecting input file!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
