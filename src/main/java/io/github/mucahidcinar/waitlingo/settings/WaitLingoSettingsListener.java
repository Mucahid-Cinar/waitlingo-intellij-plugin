package io.github.mucahidcinar.waitlingo.settings;

import com.intellij.util.messages.Topic;

public interface WaitLingoSettingsListener {
    Topic<WaitLingoSettingsListener> TOPIC = Topic.create("WaitLingo settings", WaitLingoSettingsListener.class);
    void settingsChanged();
}
