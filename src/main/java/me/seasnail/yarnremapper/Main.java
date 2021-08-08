package me.seasnail.yarnremapper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.fabricmc.mapping.reader.v2.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static final File DIR = new File(System.getProperty("user.home"), ".yarn-remapper");

    public static Map<String, String> MAPPINGS;

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        // Read input info
        System.out.println("Please input the path to the remap jar.");

        String path = new BufferedReader(new InputStreamReader(System.in)).readLine();
        File target = new File(path);

        if (!target.exists()) {
            System.out.println("That file does not exist.");
            System.exit(0);
        }

        if (!target.getName().endsWith(".jar")) {
            System.out.println("Invalid file input (must be a jar file).");
            System.exit(0);
        }

        System.out.println("Please input the Minecraft version you wish to remap to.");

        String mcVersion = new BufferedReader(new InputStreamReader(System.in)).readLine();
        int latestYarnBuild = fetchLatestYarnBuild(mcVersion);

        File mcVersionFolder = new File(DIR, mcVersion);
        if (mcVersionFolder.exists()) {
            int latestBuild = 0;

            for (File file : Objects.requireNonNull(mcVersionFolder.listFiles())) {
                if (file.getName().endsWith(".tiny")) {
                    int build = Integer.parseInt(file.getName().substring(0, 1));

                    if (build > latestBuild) latestBuild = build;
                }
            }

            if (latestBuild > latestYarnBuild) latestYarnBuild = latestBuild;
        }

        cacheMappings(mcVersion, latestYarnBuild);

        File mappingsFile = new File(mcVersionFolder, latestYarnBuild + ".tiny");
        if (!mappingsFile.exists()) {
            System.out.println("Failed to fetch mappings!");
            System.exit(0);
        }

        // Load mappings
        Main.MAPPINGS = readMappings(mappingsFile);
        System.out.println("Successfully loaded mappings.");
    }

    public static int fetchLatestYarnBuild(String mcVersion) throws IOException, InterruptedException, URISyntaxException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(new URI("https://meta.fabricmc.net/v2/versions/yarn/" + mcVersion))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json");

        InputStream res = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofInputStream()).body();

        List<YarnVersion> versions = new Gson().fromJson(
                new InputStreamReader(res),
                new TypeToken<List<YarnVersion>>() {}.getType()
        );

        versions.sort(Comparator.comparingInt(version -> version.build));
        Collections.reverse(versions);

        if (!versions.isEmpty()) return versions.get(0).build;
        else return 0;
    }

    public static void cacheMappings(String mcVersion, int yarnBuild) throws IOException {
        File mcVersionFolder = new File(DIR, mcVersion);

        // Delete past cache because there is a newer version available
        if (mcVersionFolder.exists()) FileUtils.cleanDirectory(mcVersionFolder);
        else mcVersionFolder.mkdir();

        String yarnVersion = mcVersion + "+build." + yarnBuild;

        // Download yarn mappings
        System.out.printf("Downloading Yarn mappings for Minecraft %s (Build %d)...", mcVersion, yarnBuild);

        File jarFile = new File(mcVersionFolder, "yarn.jar");
        if (jarFile.exists()) jarFile.delete();

        jarFile.deleteOnExit();

        try {
            FileUtils.copyURLToFile(new URL("https://maven.fabricmc.net/net/fabricmc/yarn/" + yarnVersion + "/yarn-" + yarnVersion + "-v2.jar"), jarFile);
            System.out.println("Complete.");
        } catch (IOException e) {
            System.out.println("Failed.");
            System.exit(0);
            return;
        }

        // Extract mappings from jar
        System.out.print("Extracting tiny mappings from Yarn jar...");

        File mappingsFile = new File(mcVersionFolder, yarnBuild + ".tiny");

        try (FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
            Files.copy(jar.getPath("mappings/mappings.tiny"), mappingsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Complete.");
        } catch (IOException e) {
            System.out.println("Failed");
            System.exit(0);
            return;
        }

        jarFile.delete();
    }

    public static Map<String, String> readMappings(File file) throws IOException {
        Map<String, String> mappings = new HashMap<>();

        TinyV2Factory.visit(Files.newBufferedReader(file.toPath()), new TinyVisitor() {
            private final Map<String, Integer> namespaceStringToColumn = new HashMap<>();

            private void addMappings(MappingGetter name) {
                mappings.put(
                        name.get(namespaceStringToColumn.get("intermediary")).replace('/', '.'),
                        name.get(namespaceStringToColumn.get("named")).replace('/', '.')
                );
            }

            @Override
            public void start(TinyMetadata metadata) {
                namespaceStringToColumn.put("intermediary", metadata.index("intermediary"));
                namespaceStringToColumn.put("named", metadata.index("named"));
            }

            @Override
            public void pushClass(MappingGetter name) {
                addMappings(name);
            }

            @Override
            public void pushMethod(MappingGetter name, String descriptor) {
                addMappings(name);
            }

            @Override
            public void pushField(MappingGetter name, String descriptor) {
                addMappings(name);
            }
        });

        return mappings;
    }

    public static class YarnVersion {
        public int build;
    }
}
