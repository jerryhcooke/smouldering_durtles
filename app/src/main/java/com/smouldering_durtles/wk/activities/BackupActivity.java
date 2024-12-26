package com.smouldering_durtles.wk.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smouldering_durtles.wk.GlobalSettings;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import javax.annotation.Nullable;

public class BackupActivity extends AppCompatActivity {
    private static final String TAG = "BackupActivity";
    private ActivityResultLauncher<Intent> saveFileLauncher;
    private ActivityResultLauncher<Intent> pickFileLauncher;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the result callbacks
        saveFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleSaveFileResult(Objects.requireNonNull(result.getData().getData()));
                    }
                }
        );

        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handlePickFileResult(Objects.requireNonNull(result.getData().getData()));
                    }
                }
        );

        // Determine the action to perform
        String action = Objects.requireNonNull(getIntent().getAction());
        if ("com.smouldering_durtles.wk.BACKUP".equals(action)) {
            startBackup();
        } else if ("com.smouldering_durtles.wk.RESTORE".equals(action)) {
            startRestore();
        } else {
            Log.e(TAG, "Unknown action: " + action);
            finish();
        }
    }
    private String generateBackupFilename() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return "smouldering_durtles_" + dateFormat.format(new Date()) + ".json";
    }

    private void startBackup() {
        String backupFilename = generateBackupFilename();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, backupFilename);
        saveFileLauncher.launch(intent);
    }

    private void startRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        pickFileLauncher.launch(intent);
    }

    private void handleSaveFileResult(Uri uri) {
        try {
            OutputStream outputStream = Objects.requireNonNull(getContentResolver().openOutputStream(uri));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            Gson gson = new Gson();

            Map<String, ?> allEntries = GlobalSettings.getAllSettings();
            Map<String, Map<String, String>> allEntriesWithTypes = new HashMap<>();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                Map<String, String> item = new HashMap<>();
                item.put("value", entry.getValue().toString());
                item.put("type", entry.getValue().getClass().getSimpleName());
                allEntriesWithTypes.put(entry.getKey(), item);
            }

            writer.write(gson.toJson(allEntriesWithTypes));
            writer.close();
            outputStream.close();

            Toast.makeText(this, "Settings backed up successfully.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePickFileResult(Uri uri) {
        try {
            InputStream inputStream = Objects.requireNonNull(getContentResolver().openInputStream(uri));
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Gson gson = new Gson();

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            Type type = new TypeToken<HashMap<String, Map<String, String>>>() {}.getType();
            Map<String, Map<String, String>> settingsWithTypes = gson.fromJson(stringBuilder.toString(), type);

            Map<String, Object> settings = new HashMap<>();
            for (Map.Entry<String, Map<String, String>> entry : settingsWithTypes.entrySet()) {
                String key = entry.getKey();
                String value = Objects.requireNonNull(entry.getValue().get("value"));
                String typeStr = Objects.requireNonNull(entry.getValue().get("type"));

                Object convertedValue;
                switch (typeStr) {
                    case "Integer":
                        convertedValue = Integer.valueOf(value);
                        break;
                    case "Boolean":
                        convertedValue = Boolean.valueOf(value);
                        break;
                    case "Float":
                        convertedValue = Float.valueOf(value);
                        break;
                    case "Long":
                        convertedValue = Long.valueOf(value);
                        break;
                    case "String":
                        convertedValue = value;
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported type: " + typeStr);
                }

                settings.put(key, convertedValue);
            }

            GlobalSettings.setAllSettings(settings);
            reader.close();
            inputStream.close();
            Toast.makeText(this, "Settings restored successfully.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}