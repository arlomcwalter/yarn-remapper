package me.seasnail.yarnremapper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class Utils {
    public static void populateMcVers(JComboBox<String> mcSelector, boolean snapshots, JComboBox<String> yarnSelector) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
            .uri(new URI("https://meta.fabricmc.net/v2/versions/game/"))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .header("Accept", "application/json");

        InputStream res = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofInputStream()).body();

        List<Main.MinecraftVersion> versions = new Gson().fromJson(
            new InputStreamReader(res),
            new TypeToken<List<Main.MinecraftVersion>>() {
            }.getType()
        );

        for (Main.MinecraftVersion version : versions) {
            if (version.stable || snapshots) mcSelector.addItem(version.version);
        }

        populateYarnVers(yarnSelector, (String) mcSelector.getSelectedItem());
    }

    public static void populateYarnVers(JComboBox<String> dropDown, String mcVersion) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
            .uri(new URI("https://meta.fabricmc.net/v2/versions/yarn/" + mcVersion))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .header("Accept", "application/json");

        InputStream res = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofInputStream()).body();

        List<Main.YarnVersion> versions = new Gson().fromJson(
            new InputStreamReader(res),
            new TypeToken<List<Main.YarnVersion>>() {
            }.getType()
        );

        for (Main.YarnVersion version : versions) {
            dropDown.addItem("build-" + version.build);
        }
    }
}
