package com.chasmet.dcomplexgpt;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends android.app.Activity {
    private AppPrefs prefs;
    private UpdateManager updateManager;
    private McpClient mcpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new AppPrefs(this);
        updateManager = new UpdateManager(this);
        mcpClient = new McpClient();

        TextView versionHome = findViewById(R.id.versionHome);
        Button settingsButton = findViewById(R.id.settingsButton);

        versionHome.setText("Version " + getVersionName());
        settingsButton.setOnClickListener(v -> showSettings());

        if (prefs.isAutoUpdateEnabled()) {
            updateManager.check(false);
        }
    }

    private void showSettings() {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_settings, null, false);

        TextView versionText = view.findViewById(R.id.versionText);
        EditText mcpUrlInput = view.findViewById(R.id.mcpUrlInput);
        EditText mcpTokenInput = view.findViewById(R.id.mcpTokenInput);
        Button testMcpButton = view.findViewById(R.id.testMcpButton);
        TextView mcpStatusText = view.findViewById(R.id.mcpStatusText);
        CheckBox autoUpdateCheck = view.findViewById(R.id.autoUpdateCheck);
        Button checkUpdateButton = view.findViewById(R.id.checkUpdateButton);

        versionText.setText("Version installée : " + getVersionName());
        mcpUrlInput.setText(prefs.getMcpUrl());
        mcpTokenInput.setText(prefs.getMcpToken());
        autoUpdateCheck.setChecked(prefs.isAutoUpdateEnabled());

        testMcpButton.setOnClickListener(v -> {
            String url = mcpUrlInput.getText().toString().trim();
            String token = mcpTokenInput.getText().toString().trim();

            if (TextUtils.isEmpty(url)) {
                mcpStatusText.setText("Renseigne l’URL du serveur MCP.");
                return;
            }

            mcpStatusText.setText("Connexion MCP…");
            testMcpButton.setEnabled(false);

            mcpClient.testConnection(url, token, new McpClient.Callback() {
                @Override
                public void onSuccess(String status) {
                    runOnUiThread(() -> {
                        mcpStatusText.setText(status);
                        testMcpButton.setEnabled(true);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        mcpStatusText.setText("Erreur MCP : " + error);
                        testMcpButton.setEnabled(true);
                    });
                }
            });
        });

        checkUpdateButton.setOnClickListener(v -> updateManager.check(true));

        new AlertDialog.Builder(this)
                .setTitle("Réglages")
                .setView(view)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    prefs.setMcpUrl(mcpUrlInput.getText().toString());
                    prefs.setMcpToken(mcpTokenInput.getText().toString());
                    prefs.setAutoUpdateEnabled(autoUpdateCheck.isChecked());

                    Toast.makeText(
                            this,
                            "Réglages MCP enregistrés",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .show();
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "inconnue";
        }
    }
}
