package com.chasmet.dcomplexgpt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpdateManager {
    private static final String LATEST_RELEASE =
            "https://api.github.com/repos/Chasmet/D-complex-gpt/releases/latest";

    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UpdateManager(Activity activity) {
        this.activity = activity;
    }

    public void check(boolean manual) {
        executor.execute(() -> {
            try {
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(LATEST_RELEASE).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "D-Complex-GPT-Android");

                int code = connection.getResponseCode();
                if (code == 404) {
                    if (manual) toast("Aucune version publiée pour le moment.");
                    return;
                }
                if (code < 200 || code >= 300) {
                    if (manual) toast("Vérification impossible : HTTP " + code);
                    return;
                }

                JSONObject release = new JSONObject(readText(connection.getInputStream()));
                String latest = release.optString("tag_name", "").replaceFirst("^[vV]", "");
                String currentDisplay = getCurrentVersion();
                String current = currentDisplay.replace("-debug", "");

                String apkUrl = "";
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.optString("name", "");
                        if (name.toLowerCase().endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url", "");
                            break;
                        }
                    }
                }

                if (latest.isEmpty() || apkUrl.isEmpty() || compareVersions(latest, current) <= 0) {
                    if (manual) toast("Application à jour (" + currentDisplay + ").");
                    return;
                }

                String finalApkUrl = apkUrl;
                activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                        .setTitle("Mise à jour disponible")
                        .setMessage("Version " + latest + " disponible. Installer la nouvelle version ?")
                        .setNegativeButton("Plus tard", null)
                        .setPositiveButton("Télécharger",
                                (dialog, which) -> downloadAndInstall(finalApkUrl))
                        .show());

            } catch (Exception e) {
                if (manual) toast("Erreur : " + safe(e.getMessage()));
            }
        });
    }

    private void downloadAndInstall(String apkUrl) {
        executor.execute(() -> {
            try {
                File dir = activity.getExternalFilesDir("updates");
                if (dir == null) {
                    toast("Stockage indisponible.");
                    return;
                }
                if (!dir.exists() && !dir.mkdirs()) {
                    toast("Impossible de préparer le dossier de mise à jour.");
                    return;
                }

                File apk = new File(dir, "dcomplex-update.apk");

                HttpURLConnection connection =
                        (HttpURLConnection) new URL(apkUrl).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setRequestProperty("User-Agent", "D-Complex-GPT-Android");

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    toast("Téléchargement impossible : HTTP " + code);
                    return;
                }

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }

                activity.runOnUiThread(() -> install(apk));
            } catch (Exception e) {
                toast("Téléchargement impossible : " + safe(e.getMessage()));
            }
        });
    }

    private void install(File apk) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Autorisation nécessaire")
                    .setMessage("Autorise D-Complex GPT à installer ses mises à jour, puis relance la vérification.")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Ouvrir le réglage", (dialog, which) -> {
                        Intent settings = new Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName())
                        );
                        activity.startActivity(settings);
                    })
                    .show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".fileprovider",
                apk
        );

        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(install);
    }

    private static String readText(InputStream input) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String getCurrentVersion() {
        try {
            return activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    private void toast(String message) {
        activity.runOnUiThread(() ->
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show());
    }

    private static int compareVersions(String leftVersion, String rightVersion) {
        String[] left = leftVersion.split("[^0-9]+");
        String[] right = rightVersion.split("[^0-9]+");
        int size = Math.max(left.length, right.length);

        for (int i = 0; i < size; i++) {
            int a = i < left.length && !left[i].isEmpty() ? parse(left[i]) : 0;
            int b = i < right.length && !right[i].isEmpty() ? parse(right[i]) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private static int parse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Erreur inconnue" : value;
    }
}
