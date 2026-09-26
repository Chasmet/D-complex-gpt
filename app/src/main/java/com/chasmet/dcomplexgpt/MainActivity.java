package com.chasmet.dcomplexgpt;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public final class MainActivity extends android.app.Activity {
    private AppPrefs prefs;
    private UpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new AppPrefs(this);
        updateManager = new UpdateManager(this);

        TextView versionHome = findViewById(R.id.versionHome);
        Button settingsButton = findViewById(R.id.settingsButton);

        versionHome.setText("Version " + BuildConfig.VERSION_NAME);
        settingsButton.setOnClickListener(v -> showSettings());

        if (prefs.isAutoUpdateEnabled()) {
            updateManager.check(false);
        }
    }

    private void showSettings() {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_settings, null, false);

        TextView versionText = view.findViewById(R.id.versionText);
        CheckBox autoUpdateCheck = view.findViewById(R.id.autoUpdateCheck);
        Button checkUpdateButton = view.findViewById(R.id.checkUpdateButton);

        versionText.setText("Version installée : " + BuildConfig.VERSION_NAME);
        autoUpdateCheck.setChecked(prefs.isAutoUpdateEnabled());
        checkUpdateButton.setOnClickListener(v -> updateManager.check(true));

        new AlertDialog.Builder(this)
                .setTitle("Réglages")
                .setView(view)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    prefs.setAutoUpdateEnabled(autoUpdateCheck.isChecked());
                    Toast.makeText(
                            this,
                            autoUpdateCheck.isChecked()
                                    ? "Mise à jour automatique activée"
                                    : "Mise à jour automatique désactivée",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .show();
    }
}
