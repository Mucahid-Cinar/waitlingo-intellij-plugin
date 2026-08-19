package io.github.mucahidcinar.waitlingo.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.APP)
@State(name = "WaitLingoSettings", storages = @Storage("waitlingo.xml"))
public final class WaitLingoSettings implements PersistentStateComponent<WaitLingoSettings.Data> {

    public static final String EN_TO_TR = "EN_TR";
    public static final String TR_TO_EN = "TR_EN";

    public static final class Data {
        public int rotationSeconds = 12;
        public String direction = EN_TO_TR;
        public List<String> levels = new ArrayList<>(List.of("A2", "B1"));
        public List<String> topics = new ArrayList<>();
        public int fontPercent = 100;
        /** Kept for settings-file compatibility; interactive quizzes are intentionally disabled. */
        public boolean quizEnabled = false;
        public boolean notificationsEnabled = false;
        public boolean developerWordsEnabled = true;
        public String customVocabularyPath = "";
        public boolean compactMode = false;
        public boolean adaptiveRotation = true;
        public String contentPack = "ALL";
    }

    private Data data = new Data();

    public static WaitLingoSettings getInstance() {
        return ApplicationManager.getApplication().getService(WaitLingoSettings.class);
    }

    @Override
    public Data getState() {
        return data;
    }

    @Override
    public void loadState(@NotNull Data state) {
        data = state;
        normalize();
    }

    public void update(Data newData) {
        data = newData;
        normalize();
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(WaitLingoSettingsListener.TOPIC).settingsChanged();
    }

    private void normalize() {
        data.rotationSeconds = Math.max(5, Math.min(3600, data.rotationSeconds));
        data.fontPercent = Math.max(80, Math.min(160, data.fontPercent));
        if (!TR_TO_EN.equals(data.direction)) data.direction = EN_TO_TR;
        data.quizEnabled = false;
        if (data.levels == null) data.levels = new ArrayList<>();
        if (data.topics == null) data.topics = new ArrayList<>();
    }
}
