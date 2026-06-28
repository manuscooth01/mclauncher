package com.miempresa.mclauncher;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> versionNames = new ArrayList<>();
    private final ArrayList<String> versionUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, versionNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String url = versionUrls.get(position);
            String name = versionNames.get(position);
            downloadVersionJson(url, name);
        });

        loadVersionList();
    }

    private void loadVersionList() {
        new Thread(() -> {
            try {
                URL url = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject manifest = new JSONObject(sb.toString());
                JSONArray versions = manifest.getJSONArray("versions");

                versionNames.clear();
                versionUrls.clear();

                for (int i = 0; i < versions.length(); i++) {
                    JSONObject v = versions.getJSONObject(i);
                    String id = v.getString("id");
                    String type = v.getString("type");
                    versionNames.add(id + " [" + type + "]");
                    versionUrls.add(v.getString("url"));
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void downloadVersionJson(String urlStr, String displayName) {
        new Thread(() -> {
            try {
                String versionId = displayName.split(" \\[")[0];
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                File dir = new File(getFilesDir(), "versions/" + versionId);
                if (!dir.exists()) dir.mkdirs();

                File outFile = new File(dir, versionId + ".json");
                FileWriter writer = new FileWriter(outFile);
                writer.write(sb.toString());
                writer.close();

                runOnUiThread(() -> Toast.makeText(this, "Guardado: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error descarga: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
