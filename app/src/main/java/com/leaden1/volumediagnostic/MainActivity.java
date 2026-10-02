package com.leaden1.volumediagnostic;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView statusText, mediaSessionText, volumeText;
    private TextView keyEventText, lastChangeText, historyText;

    private AudioManager audioManager;
    private MediaSessionManager mediaSessionManager;
    private Handler handler;

    private int lastMusicVolume = -1;
    private String lastMediaPackage = "";
    private final StringBuilder history = new StringBuilder();

    private final Runnable volumePoller = new Runnable() {
        @Override
        public void run() {
            checkVolumeChange();
            updateVolumeDisplay();
            handler.postDelayed(this, 250);
        }
    };

    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logEvent("BROADCAST_VOLUME: " + intent.getAction());
            checkVolumeChange();
        }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener
            sessionsListener = controllers -> {
        logEvent("SESIONES_CAMBIARON: " +
                (controllers == null ? 0 : controllers.size()));
        refreshMediaSessions();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mediaSessionManager =
                (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        handler = new Handler(Looper.getMainLooper());

        buildInterface();
        registerVolumeReceiver();

        lastMusicVolume =
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        logEvent("APP_INICIADA_V5_1");
        refreshAll();
        handler.post(volumePoller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();

        if (MediaNotificationListener.isConnected()) {
            try {
                mediaSessionManager.addOnActiveSessionsChangedListener(
                        sessionsListener, getListenerComponent());
                logEvent("MEDIA_SESSION_LISTENER_ACTIVO");
            } catch (SecurityException e) {
                logEvent("SECURITY_EXCEPTION_LISTENER: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(
                    sessionsListener);
        } catch (Exception ignored) {
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(volumePoller);
        try {
            unregisterReceiver(volumeReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        statusText = createText("● INICIANDO MONITOR...");
        mediaSessionText = createText("MEDIA SESSION: ...");
        volumeText = createText("STREAM_MUSIC: ...");
        keyEventText = createText("ÚLTIMO KEY EVENT: —");
        lastChangeText = createText("ÚLTIMO CAMBIO: —");

        Button accessButton = new Button(this);
        accessButton.setText("ABRIR ACCESO A NOTIFICACIONES");
        accessButton.setOnClickListener(v -> openNotificationAccess());

        Button updateButton = new Button(this);
        updateButton.setText("ACTUALIZAR SESIONES");
        updateButton.setOnClickListener(v -> refreshAll());

        Button clearButton = new Button(this);
        clearButton.setText("LIMPIAR HISTORIAL");
        clearButton.setOnClickListener(v -> {
            history.setLength(0);
            historyText.setText("HISTORIAL\n");
        });

        historyText = createText("HISTORIAL\n");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(historyText);

        root.addView(statusText);
        root.addView(mediaSessionText);
        root.addView(volumeText);
        root.addView(keyEventText);
        root.addView(lastChangeText);
        root.addView(accessButton);
        root.addView(updateButton);
        root.addView(clearButton);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
    }

    private TextView createText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setPadding(0, 10, 0, 10);
        return tv;
    }

    private void registerVolumeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.media.STREAM_VOLUME_CHANGED_ACTION");

        try {
            registerReceiver(volumeReceiver, filter);
        } catch (Exception e) {
            logEvent("REGISTER_RECEIVER_ERROR: " + e.getMessage());
        }
    }

    private void checkVolumeChange() {
        int current =
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (lastMusicVolume == -1) {
            lastMusicVolume = current;
            return;
        }

        if (current != lastMusicVolume) {
            int old = lastMusicVolume;
            lastMusicVolume = current;

            String direction =
                    current > old ? "VOLUME_UP" : "VOLUME_DOWN";

            String event =
                    "CAMBIO_VOLUME " + old + " -> " + current +
                    " [" + direction + "]";

            lastChangeText.setText("ÚLTIMO CAMBIO: " + event);
            logEvent(event);
        }
    }

    private void updateVolumeDisplay() {
        int current =
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max =
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        volumeText.setText(
                "STREAM_MUSIC: " + current + " / " + max);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            String name = KeyEvent.keyCodeToString(event.getKeyCode());
            String detail = name + " (" + event.getKeyCode() + ")";

            keyEventText.setText("ÚLTIMO KEY EVENT: " + detail);
            logEvent("KEY_DOWN: " + detail);
        }

        return super.dispatchKeyEvent(event);
    }

    private void refreshAll() {
        updateAccessStatus();
        updateVolumeDisplay();
        refreshMediaSessions();
    }

    private void updateAccessStatus() {
        if (MediaNotificationListener.isConnected()) {
            statusText.setText("● MONITOR ACTIVO");
        } else {
            statusText.setText(
                    "● ACCESO A NOTIFICACIONES NO ACTIVO");
        }
    }

    private void refreshMediaSessions() {
        if (!MediaNotificationListener.isConnected()) {
            mediaSessionText.setText(
                    "MEDIA SESSION: ACCESO NO DISPONIBLE\n" +
                    "Activa el acceso de notificaciones.");
            return;
        }

        try {
            List<MediaController> sessions =
                    mediaSessionManager.getActiveSessions(
                            getListenerComponent());

            if (sessions == null || sessions.isEmpty()) {
                mediaSessionText.setText(
                        "MEDIA SESSION: SESIONES_ENCONTRADAS=0");
                lastMediaPackage = "";
                return;
            }

            MediaController selected = sessions.get(0);
            String packageName = selected.getPackageName();

            String state =
                    selected.getPlaybackState() == null
                            ? "—"
                            : String.valueOf(
                                    selected.getPlaybackState().getState());

            mediaSessionText.setText(
                    "MEDIA SESSION: " + packageName +
                    "\nESTADO: " + state);

            if (!packageName.equals(lastMediaPackage)) {
                lastMediaPackage = packageName;
                logEvent(
                        "MEDIA_KEY_SESSION_PACKAGE=" + packageName);
            }

        } catch (SecurityException e) {
            mediaSessionText.setText(
                    "MEDIA SESSION: SECURITY_EXCEPTION\n" +
                    e.getMessage());
            logEvent("SECURITY_EXCEPTION: " + e.getMessage());

        } catch (Exception e) {
            mediaSessionText.setText(
                    "MEDIA SESSION: ERROR\n" + e.getMessage());
            logEvent("MEDIA_SESSION_ERROR: " + e.getMessage());
        }
    }

    private ComponentName getListenerComponent() {
        return new ComponentName(
                this, MediaNotificationListener.class);
    }

    private void openNotificationAccess() {
        try {
            Intent intent =
                    new Intent(
                            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
            logEvent("ABRIENDO_ACCESO_NOTIFICACIONES");
        } catch (Exception e) {
            logEvent(
                    "NO_SE_PUDO_ABRIR_SETTINGS: " + e.getMessage());
        }
    }

    private void logEvent(String event) {
        String time =
                new java.text.SimpleDateFormat(
                        "HH:mm:ss.SSS",
                        Locale.getDefault())
                        .format(new java.util.Date());

        String line = time + "  " + event;
        history.insert(0, line + "\n");

        if (historyText != null) {
            historyText.setText(
                    "HISTORIAL\n\n" + history);
        }
    }
}
