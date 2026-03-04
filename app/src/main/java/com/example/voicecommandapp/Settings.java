package com.example.voicecommandapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Settings extends AppCompatActivity implements TextToSpeech.OnInitListener {

    SwitchCompat switchVoice, switchVibration, switchDarkMode;
    Spinner spinnerVoice;
    SeekBar seekBarPitch, seekBarSpeed;
    EditText etEmergencyNumber, etUserName;
    Button btnTestVoice, btnSaveSettings, btnClearHistory;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    TextToSpeech textToSpeech;
    
    private List<String> languageNames = new ArrayList<>();
    private List<Locale> locales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Initialize Views
        etUserName = findViewById(R.id.etUserName);
        switchVoice = findViewById(R.id.switchVoice);
        switchVibration = findViewById(R.id.switchVibration);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        spinnerVoice = findViewById(R.id.spinnerVoice);
        seekBarPitch = findViewById(R.id.seekBarPitch);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        etEmergencyNumber = findViewById(R.id.etEmergencyNumber);
        btnTestVoice = findViewById(R.id.btnTestVoice);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        preferences = getSharedPreferences("VoiceMateSettings", MODE_PRIVATE);
        editor = preferences.edit();

        textToSpeech = new TextToSpeech(this, this);

        setupVoiceSelection();
        loadSettings();

        // Listeners
        btnTestVoice.setOnClickListener(v -> speakTest());
        btnSaveSettings.setOnClickListener(v -> saveSettings());
        
        btnClearHistory.setOnClickListener(v -> {
            Toast.makeText(this, "Command History Cleared", Toast.LENGTH_SHORT).show();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void setupVoiceSelection() {
        languageNames.clear();
        locales.clear();
        
        languageNames.add("English (US)");
        locales.add(Locale.US);
        
        languageNames.add("English (UK)");
        locales.add(Locale.UK);
        
        languageNames.add("Hindi (India)");
        locales.add(new Locale("hi", "IN"));
        
        languageNames.add("French (France)");
        locales.add(Locale.FRANCE);
        
        languageNames.add("German (Germany)");
        locales.add(Locale.GERMANY);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, languageNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVoice.setAdapter(adapter);
    }

    private void loadSettings() {
        etUserName.setText(preferences.getString("user_name", ""));
        switchVoice.setChecked(preferences.getBoolean("voice_enabled", true));
        switchVibration.setChecked(preferences.getBoolean("vibration_enabled", true));
        switchDarkMode.setChecked(preferences.getBoolean("dark_mode", false));
        
        seekBarPitch.setProgress(preferences.getInt("voice_pitch", 50));
        seekBarSpeed.setProgress(preferences.getInt("voice_speed", 50));
        
        etEmergencyNumber.setText(preferences.getString("emergency_number", "100"));

        String langCode = preferences.getString("voice_lang", "en_US");
        for (int i = 0; i < locales.size(); i++) {
            if (locales.get(i).toString().equals(langCode)) {
                spinnerVoice.setSelection(i);
                break;
            }
        }
    }

    private void saveSettings() {
        editor.putString("user_name", etUserName.getText().toString());
        editor.putBoolean("voice_enabled", switchVoice.isChecked());
        editor.putBoolean("vibration_enabled", switchVibration.isChecked());
        editor.putBoolean("dark_mode", switchDarkMode.isChecked());
        editor.putInt("voice_pitch", seekBarPitch.getProgress());
        editor.putInt("voice_speed", seekBarSpeed.getProgress());
        editor.putString("emergency_number", etEmergencyNumber.getText().toString());
        
        int selectedIndex = spinnerVoice.getSelectedItemPosition();
        editor.putString("voice_lang", locales.get(selectedIndex).toString());
        
        editor.apply();
        Toast.makeText(this, "Settings Saved Successfully", Toast.LENGTH_SHORT).show();
    }

    private void speakTest() {
        if (textToSpeech != null) {
            float pitch = (float) seekBarPitch.getProgress() / 50.0f;
            if (pitch < 0.1) pitch = 0.1f;
            
            float speed = (float) seekBarSpeed.getProgress() / 50.0f;
            if (speed < 0.1) speed = 0.1f;

            textToSpeech.setPitch(pitch);
            textToSpeech.setSpeechRate(speed);
            
            int selectedIndex = spinnerVoice.getSelectedItemPosition();
            textToSpeech.setLanguage(locales.get(selectedIndex));

            String name = etUserName.getText().toString();
            String greeting = name.isEmpty() ? "Testing settings." : "Hello " + name ;
            
            textToSpeech.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onInit(int status) {
        // Init handled
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}