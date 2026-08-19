package io.github.mucahidcinar.waitlingo.audio;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;

public final class PronunciationService {
    private static final Logger LOG = Logger.getInstance(PronunciationService.class);
    private PronunciationService() {}

    public static void speak(String text) {
        if (text == null || text.isBlank()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ProcessBuilder builder;
                if (SystemInfo.isWindows) {
                    String safe = text.replace("'", "''");
                    builder = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command",
                            "Add-Type -AssemblyName System.Speech; $s=New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Speak('" + safe + "')");
                } else if (SystemInfo.isMac) {
                    builder = new ProcessBuilder("say", text);
                } else {
                    builder = new ProcessBuilder("espeak", text);
                }
                builder.redirectErrorStream(true).start();
            } catch (Exception exception) {
                LOG.info("Offline text-to-speech is unavailable", exception);
            }
        });
    }
}
