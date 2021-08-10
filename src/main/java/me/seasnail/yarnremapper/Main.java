package me.seasnail.yarnremapper;

import com.formdev.flatlaf.FlatDarculaLaf;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.apache.commons.io.FileUtils;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {
    public static final File CACHE_DIR = new File(System.getProperty("user.home"), ".yarn-remapper");
    public static GUI gui;

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        FlatDarculaLaf.setup();
        gui = new GUI();
    }

    public static void remap(File input, File output, String mcVersion, int yarnBuild, boolean headless) throws IOException, URISyntaxException, InterruptedException {
        if (!input.exists() || !input.getName().endsWith(".jar")) {
            TinyFileDialogs.tinyfd_messageBox("Error reading input JAR!", "The input JAR could not be read, please check it is correct.", "ok", "error", false);
            return;
        }

        if (!output.getName().endsWith(".jar") || !output.getParentFile().isDirectory()) {
            TinyFileDialogs.tinyfd_messageBox("Invalid output path!", "The specified output is not vaild.", "ok", "error", false);
            return;
        }

        File mcVersionFolder = new File(CACHE_DIR, mcVersion);
        String yarnVersion = mcVersion + "+build." + yarnBuild;

        if (!mcVersionFolder.exists()) mcVersionFolder.mkdir();

        File mappingsFile = new File(mcVersionFolder, yarnBuild + ".tiny");
        if (!mappingsFile.exists()) {
            // Download yarn mappings
            System.out.printf("Downloading Yarn mappings for Minecraft %s (Build %d)...", mcVersion, yarnBuild);

            File jarFile = new File(mcVersionFolder, "yarn.jar");
            if (jarFile.exists()) jarFile.delete();

            jarFile.deleteOnExit();

            try {
                FileUtils.copyURLToFile(new URL("https://maven.fabricmc.net/net/fabricmc/yarn/" + yarnVersion + "/yarn-" + yarnVersion + "-v2.jar"), jarFile);
                System.out.println("complete!");
            } catch (IOException e) {
                System.out.println("failed!");
                if (!headless) TinyFileDialogs.tinyfd_messageBox("Error downloading mappings!", "Mappings may be corrupted or incomplete.", "ok", "error", false);
                return;
            }

            // Extract mappings from jar
            System.out.print("Extracting tiny mappings from Yarn jar...");

            try (FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
                Files.copy(jar.getPath("mappings/mappings.tiny"), mappingsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("complete!");
            } catch (IOException e) {
                System.out.println("failed!");
                if (!headless) TinyFileDialogs.tinyfd_messageBox("Error extracting mappings!", "Please try again.", "ok", "error", false);
                return;
            }

            jarFile.delete();
        }

        if (!mappingsFile.exists()) {
            System.out.println("Error fetching mappings, Please check the versions you have selected are correct.");
            if (!headless) TinyFileDialogs.tinyfd_messageBox("Error extracting mappings!", "Please try again.", "ok", "error", false);
            return;
        }

        System.out.printf("Remapping %s to %s...", input.getName(), output.getAbsolutePath());

        TinyRemapper remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappingsFile.toPath(), "intermediary", "named"))
            .build();

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output.toPath()).build()) {
            outputConsumer.addNonClassFiles(input.toPath(), NonClassCopyMode.FIX_META_INF, remapper);

            remapper.readInputs(input.toPath());
            remapper.apply(outputConsumer);
        } catch (IOException e) {
            System.out.println("failed!");
            throw new RuntimeException(e);
        } finally {
            remapper.finish();
            System.out.println("complete!");
            if (!headless) TinyFileDialogs.tinyfd_messageBox("Complete!", "Successfully remapped JAR.", "ok", "info", false);
        }
    }
}
