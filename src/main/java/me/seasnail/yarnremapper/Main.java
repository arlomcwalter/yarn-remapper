package me.seasnail.yarnremapper;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import net.fabricmc.mapping.reader.v2.MappingGetter;
import net.fabricmc.mapping.reader.v2.TinyMetadata;
import net.fabricmc.mapping.reader.v2.TinyV2Factory;
import net.fabricmc.mapping.reader.v2.TinyVisitor;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String[] PATTERNS = new String[]{
            "(net(\\.|\\/)minecraft(\\.|\\/))([a-z]+)?([a-ln-z]{5,}_\\d+|\\.|\\\\|\\$)+(?<!\\.|\\\\|[^0-9])",
            "class_\\d+",
            "method_\\d+",
            "field_\\d+"
    };

    private static Map<String, String> mappings = null;

    public static void main(String[] args) throws IOException {
        // Read input info
        System.out.println("Please input the path to the file/folder you would like to remap.");

        String path = new BufferedReader(new InputStreamReader(System.in)).readLine();
        File TARGET_FILE = new File(path);

        if (!TARGET_FILE.exists()) {
            System.out.println("Could not locate such a file.");
            System.exit(0);
        }

        if (!TARGET_FILE.isDirectory() && !TARGET_FILE.getName().endsWith(".java")) {
            System.out.println("Invalid path or file type inputted (must be a directory, java source file or java class).");
            System.exit(0);
        }

        String mcVersion = "", yarnVersion = "";

        System.out.println("Please input the Minecraft version you wish to remap to.");

        // Fetch yarn version
        try {
            mcVersion = new BufferedReader(new InputStreamReader(System.in)).readLine();

            URL url = new URL("https://meta.fabricmc.net/v2/versions/yarn/" + mcVersion);
            URLConnection request = url.openConnection();
            request.connect();

            YarnVersion[] versions = new Gson().fromJson(new InputStreamReader((InputStream) request.getContent()), YarnVersion[].class);
            Optional<YarnVersion> yarnVer = Arrays.stream(versions).max(Comparator.comparingInt(v -> v.build));

            if (yarnVer.isPresent()) yarnVersion = yarnVer.get().version;
        } catch (IOException e) {
            System.out.println("Invalid Minecraft version input.");
            System.exit(0);
        }

        if (mcVersion.equals("") || yarnVersion.equals("")) {
            System.out.println("Invalid Minecraft version input.");
            System.exit(0);
        }

        // Download yarn mappings
        System.out.println("Downloading yarn " + yarnVersion + " mappings…");

        File jarFile = new File(TARGET_FILE.getParentFile(), "yarn-mappings.jar");

        try {
            String encodedYarnVersion = UrlEscapers.urlFragmentEscaper().escape(yarnVersion);
            String artifactUrl = "https://maven.fabricmc.net/net/fabricmc/yarn/" + encodedYarnVersion + "/yarn-" + encodedYarnVersion + "-v2.jar";
            FileUtils.copyURLToFile(new URL(artifactUrl), jarFile);
        } catch (IOException e) {
            System.out.printf("Failed to fetch yarn mappings of version: %s.", yarnVersion);
            System.exit(0);
            return;
        }

        File MAPPINGS_FILE = new File(TARGET_FILE.getParentFile(), "mappings.tiny");

        try (FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
            Files.copy(jar.getPath("mappings/mappings.tiny"), MAPPINGS_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("Failed to extract mappings!");
            System.exit(0);
            return;
        }

        jarFile.deleteOnExit();
        MAPPINGS_FILE.deleteOnExit();

        // Load mappings
        Map<String, String> mappings = new HashMap<>();

        try (BufferedReader mappingReader = Files.newBufferedReader(MAPPINGS_FILE.toPath())) {
            TinyV2Factory.visit(mappingReader, new TinyVisitor() {
                private final Map<String, Integer> namespaceStringToColumn = new HashMap<>();

                private void addMappings(MappingGetter name) {
                    mappings.put(name.get(namespaceStringToColumn.get("intermediary")).replace('/', '.'),
                            name.get(namespaceStringToColumn.get("named")).replace('/', '.'));
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

        } catch (IOException e) {
            System.out.println("Failed to load mappings!");
            System.exit(0);
        }

        Main.mappings = mappings;
        System.out.println("Successfully loaded mappings.");

        // Remap recursively for all files
        if (TARGET_FILE.isDirectory()) remapAll(TARGET_FILE);
        else remapOne(TARGET_FILE);

        System.out.println("Remapping finished, exiting.");
    }

    private static void remapAll(File input) {
        File[] contents = input.listFiles();

        if (contents == null || contents.length < 1) return;

        for (File file : contents) {
            if (file.isDirectory()) remapAll(file);
            else if (file.getName().endsWith(".java")) remapOne(file);
        }
    }

    private static void remapOne(File input) {
        String stringified = "";

        try {
            stringified = Files.readString(input.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not read " + input.getName());
        }

        for (String pattern : PATTERNS) {
            stringified = mapByRegex(pattern, stringified);
        }

        try {
            FileWriter f2 = new FileWriter(input, false);
            f2.write(stringified);
            f2.close();
            System.out.println("Remapped " + input.getName());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to write to " + input.getName());
        }
    }


    private static String mapByRegex(String regex, String stringInput) {
        if (mappings == null) return stringInput;

        String[] input = {stringInput};

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input[0]);

        while (matcher.find()) {
            String match = matcher.group(0);

            mappings.forEach((intermediary, named) -> {
                if (intermediary.contains(match)) {
                    input[0] = input[0].replace(intermediary, named.replaceAll("\\$", "\\."));
                    input[0] = input[0].replace(trimClassName(intermediary), trimClassName(named.replaceAll("\\$", "\\.")));
                }
            });
        }

        return input[0];
    }

    private static String trimClassName(String packagedClassName) {
        int lastDot = packagedClassName.lastIndexOf('.');
        if (lastDot != -1) packagedClassName = packagedClassName.substring(lastDot + 1);
        return packagedClassName.replaceAll("\\$", ".");
    }

    public static class YarnVersion {
        public int build;
        public String version;
    }
}
