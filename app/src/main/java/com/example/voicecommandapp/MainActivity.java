package com.example.voicecommandapp;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_CODE_SPEECH = 1;
    private static final int REQUEST_RECORD_AUDIO = 2;
    private static final int REQUEST_CALL_PHONE = 3;

    private MaterialCardView micCard;
    private ImageView btnMic;

    private TextView tvResult;
    private TextView tvAssText;
    private TextView tvStatus;
    ArrayList<String> commandHistory = new ArrayList<>();

    private AnimatorSet pulseAnimator;

    // Text To Speech
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        micCard = findViewById(R.id.micCard);
        btnMic = findViewById(R.id.btnMic);
        tvResult = findViewById(R.id.tvResult);
        tvAssText = findViewById(R.id.tvAssText);
        tvStatus = findViewById(R.id.tvStatus);

        // initialize TTS
        textToSpeech = new TextToSpeech(this, this);

        // Permissions
        checkPermissions();

        //button mic
        btnMic.setOnClickListener(v -> {
            vibrate();
            startVoiceInput();
        });


        ///history button
        ImageButton btnHistory = findViewById(R.id.btnHistory);

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, history.class);
            intent.putStringArrayListExtra("history", commandHistory);
            startActivity(intent);
        });

        //button emergency
        ImageButton btnEmergency = findViewById(R.id.btnEmergency);

        btnEmergency.setOnClickListener(v -> {
            makeEmergencyCall();
        });

        //button settings
        ImageButton btnSettings = findViewById(R.id.btnSettings);

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Settings.class);
            startActivity(intent);
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE},
                    100);
        }
    }

    private void makeEmergencyCall() {
        SharedPreferences preferences = getSharedPreferences("VoiceMateSettings", MODE_PRIVATE);
        String emergencyNum = preferences.getString("emergency_number", "100");

        tvAssText.setText("Calling " + emergencyNum);
        speak("Calling emergency contact");

        Toast.makeText(this, "Calling: " + emergencyNum, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + emergencyNum));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
        }
    }

    //  TTS Initialization
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            applySettingsAndSpeak(null);
        }
    }

    private void applySettingsAndSpeak(String message) {
        SharedPreferences preferences =
                getSharedPreferences("VoiceMateSettings", MODE_PRIVATE);

        boolean voiceEnabled = preferences.getBoolean("voice_enabled", true);

        if (voiceEnabled && textToSpeech != null) {
            int pitchPref = preferences.getInt("voice_pitch", 50);
            float pitch = (float) pitchPref / 50.0f;
            textToSpeech.setPitch(pitch < 0.1f ? 0.1f : pitch);

            int speedPref = preferences.getInt("voice_speed", 50);
            float speed = (float) speedPref / 50.0f;
            textToSpeech.setSpeechRate(speed < 0.1f ? 0.1f : speed);

            String langCode = preferences.getString("voice_lang", "en_US");
            String[] parts = langCode.split("_");
            Locale locale = (parts.length > 1) ? new Locale(parts[0], parts[1]) : new Locale(parts[0]);
            textToSpeech.setLanguage(locale);

            if (message != null) {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
    }

    private void speak(String message) {
        applySettingsAndSpeak(message);
    }

    private void startVoiceInput() {
        tvStatus.setText("Listening...");
        micCard.setCardBackgroundColor(getResources().getColor(R.color.glow_blue));
        micCard.setCardElevation(20f);
        startPulseAnimation();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "Speech not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = result.get(0);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault());
            String time = sdf.format(new java.util.Date());
            commandHistory.add(time + "  -  " + spokenText);

            tvResult.setText("You said: " + spokenText);
            handleCommand(spokenText.toLowerCase());
        }

        tvStatus.setText("Tap to Speak");
        micCard.setCardBackgroundColor(getResources().getColor(R.color.primary_blue));
        micCard.setCardElevation(12f);
        stopPulseAnimation();
    }

    private void handleCommand(String command) {

        SharedPreferences preferences =
                getSharedPreferences("VoiceMateSettings", MODE_PRIVATE);

        String langCode = preferences.getString("voice_lang", "en_US");
        boolean isHindi = langCode.startsWith("hi");

        if (command.contains("open camera")) {

            String response = isHindi ? "कैमरा खोल रहा हूँ" : "Opening camera";
            tvAssText.setText(response);
            speak(response);

            startActivity(new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE));
        }

        else if (command.contains("time")) {

            String currentTime = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new java.util.Date());

            String response = isHindi ?
                    "अभी समय है " + currentTime :
                    "Current time is " + currentTime;

            tvAssText.setText(response);
            speak(response);
        }

        else if (command.contains("date")) {

            String today = new java.text.SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                    .format(new java.util.Date());

            String response = isHindi ?
                    "आज की तारीख है " + today :
                    "Today's date is " + today;

            tvAssText.setText(response);
            speak(response);
        }

        else if (command.contains("joke")) {

            String[] jokesEN = {
                    "Why do programmers prefer dark mode? Because light attracts bugs.",
                    "Why did the computer go to the doctor? Because it caught a virus.",
                    "Why was the computer cold? Because it forgot to close its windows."
            };

            String[] jokesHI = {
                    "टीचर: सबसे तेज़ क्या होता है? छात्र: वाईफाई, कभी चलता है कभी नहीं.",
                    "पापा: पढ़ाई कैसी चल रही है? बेटा: जैसे फ्लाइट, कभी आती है कभी जाती है.",
                    "मोबाइल: बैटरी लो है. इंसान: चार्ज कर लो."
            };

            Random random = new Random();
            String joke = isHindi ?
                    jokesHI[random.nextInt(jokesHI.length)] :
                    jokesEN[random.nextInt(jokesEN.length)];

            String laugh = isHindi ? "हा हा हा!" : "Ha ha ha!";

            tvAssText.setText(joke + " " + laugh);
            speak(joke + " " + laugh);
        }

        else if (command.contains("open settings")) {

            String response = isHindi ? "सेटिंग्स खोल रहा हूँ" : "Opening settings";

            tvAssText.setText(response);
            speak(response);

            startActivity(new Intent(MainActivity.this, Settings.class));
        }

        else if (command.contains("open contacts")) {

            String response = isHindi ? "कॉन्टैक्ट खोल रहा हूँ" : "Opening contacts";

            tvAssText.setText(response);
            speak(response);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android.cursor.dir/contact");
            startActivity(intent);
        }

        else if (command.contains("open gallery")) {

            String response = isHindi ? "गैलरी खोल रहा हूँ" : "Opening gallery";

            tvAssText.setText(response);
            speak(response);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("image/*");
            startActivity(intent);
        }

        else if (command.contains("search")) {

            String query = command.replace("search", "").trim();

            if(query.length() == 0){

                String response = isHindi ?
                        "आप क्या सर्च करना चाहते हैं?" :
                        "What should I search?";

                tvAssText.setText(response);
                speak(response);
            }
            else{

                String response = isHindi ?
                        query + " के लिए गूगल पर खोज रहा हूँ" :
                        "Searching Google for " + query;

                tvAssText.setText(response);
                speak(response);

                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra("query", query);
                startActivity(intent);
            }
        }

        else if (command.contains("open youtube")) {

            String response = isHindi ? "यूट्यूब खोल रहा हूँ" : "Opening YouTube";

            tvAssText.setText(response);
            speak(response);

            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com")));
        }

        else if (command.contains("hello") || command.contains("hi")) {

            String response = isHindi ?
                    "नमस्ते! मैं आपकी कैसे मदद कर सकता हूँ?" :
                    "Hello! How can I help you today?";

            tvAssText.setText(response);
            speak(response);
        }

        else if (command.contains("how are you")) {

            String response = isHindi ?
                    "मैं बहुत अच्छा हूँ, पूछने के लिए धन्यवाद." :
                    "I am doing great. Thank you for asking.";

            tvAssText.setText(response);
            speak(response);
        }

        else if (command.contains("your name")) {

            String response = isHindi ?
                    "मेरा नाम वॉइस मेट है. मैं आपका वॉइस असिस्टेंट हूँ." :
                    "My name is Voice Mate. Your personal voice assistant.";

            tvAssText.setText(response);
            speak(response);
        }

        else if (command.contains("thank you") || command.contains("thanks")) {

            String response = isHindi ?
                    "कोई बात नहीं." :
                    "You're welcome.";

            tvAssText.setText(response);
            speak(response);
        }

        else {

            String response = isHindi ?
                    "माफ़ कीजिए, मैं समझ नहीं पाया." :
                    "Sorry, I didn't understand.";

            tvAssText.setText(response);
            speak(response);
        }
    }

    private void startPulseAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(micCard, "scaleX", 1f, 1.1f);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setDuration(600);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(micCard, "scaleY", 1f, 1.1f);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setDuration(600);

        pulseAnimator = new AnimatorSet();
        pulseAnimator.playTogether(scaleX, scaleY);
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            micCard.setScaleX(1f);
            micCard.setScaleY(1f);
        }
    }

    private void vibrate() {
        SharedPreferences preferences = getSharedPreferences("VoiceMateSettings", MODE_PRIVATE);
        boolean vibrationEnabled = preferences.getBoolean("vibration_enabled", true);

        if (vibrationEnabled) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(100);
            }
        }
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