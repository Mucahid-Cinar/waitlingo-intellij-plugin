package io.github.mucahidcinar.waitlingo.learning;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.time.temporal.WeekFields;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import io.github.mucahidcinar.waitlingo.settings.WaitLingoSettings;

@Service(Service.Level.APP)
@State(name = "WaitLingoProgress", storages = @Storage("waitlingo-progress.xml"))
public final class WaitLingoProgress implements PersistentStateComponent<WaitLingoProgress.Data> {

    public static final class WordProgress {
        public long lastSeenAt;
        public long dueAt;
        public double intervalDays;
        public double easeFactor = 2.5;
        public int correctCount;
        public int wrongCount;
        public boolean favorite;
    }

    public static final class Data {
        public Map<String, WordProgress> words = new HashMap<>();
        public Map<String, String> personalTranslations = new HashMap<>();
        public Map<String, String> personalExamples = new HashMap<>();
        public String lastPracticeDate = "";
        public int streak;
        public int todaySeen;
        public int todayKnown;
        public int totalKnown;
        public String pinnedWord = "";
        public long snoozedUntil;
        public String hiddenDate = "";
        public Set<String> hiddenToday = new HashSet<>();
        public String dailyWordDate = "";
        public String dailyWord = "";
        public String weekKey = "";
        public int weekSeen;
        public int weekKnown;
    }

    private Data data = new Data();

    public static WaitLingoProgress getInstance() {
        return ApplicationManager.getApplication().getService(WaitLingoProgress.class);
    }

    @Override public Data getState() { return data; }

    @Override public void loadState(@NotNull Data state) {
        data = state;
        if (data.words == null) data.words = new HashMap<>();
        if (data.personalTranslations == null) data.personalTranslations = new HashMap<>();
        if (data.personalExamples == null) data.personalExamples = new HashMap<>();
        if (data.hiddenToday == null) data.hiddenToday = new HashSet<>();
    }

    public WordProgress progress(String id) {
        touchDay();
        return data.words.computeIfAbsent(id, ignored -> new WordProgress());
    }

    public void savePersonalWord(String word, String translation, String example) {
        String id = word.trim().toLowerCase(java.util.Locale.ROOT);
        data.personalTranslations.put(id, translation.trim());
        data.personalExamples.put(id, example == null ? "" : example.trim());
    }

    public void markFavorite(String id) {
        progress(id).favorite = true;
    }

    public void recordSeen(String id) {
        WordProgress progress = progress(id);
        progress.lastSeenAt = System.currentTimeMillis();
        data.todaySeen++;
        touchWeek();
        data.weekSeen++;
    }

    public boolean hasSeen(String id) {
        WordProgress progress = data.words.get(id);
        return progress != null && progress.lastSeenAt > 0;
    }

    public void recordKnown(String id) {
        WordProgress progress = progress(id);
        boolean firstSuccessfulRecall = progress.correctCount == 0;
        progress.correctCount++;
        progress.lastSeenAt = System.currentTimeMillis();
        progress.intervalDays = progress.intervalDays <= 0 ? 1 : Math.min(180, progress.intervalDays * progress.easeFactor);
        progress.easeFactor = Math.min(3.0, progress.easeFactor + 0.1);
        progress.dueAt = progress.lastSeenAt + (long) (progress.intervalDays * 86_400_000L);
        data.todayKnown++;
        touchWeek();
        data.weekKnown++;
        if (firstSuccessfulRecall) data.totalKnown++;
        if (ApplicationManager.getApplication() != null
                && WaitLingoSettings.getInstance().getState().notificationsEnabled
                && (data.todayKnown == 5 || data.todayKnown == 10 || data.todayKnown % 25 == 0)) {
            NotificationGroupManager.getInstance().getNotificationGroup("WaitLingo Progress")
                    .createNotification("WaitLingo daily progress",
                            "You learned " + data.todayKnown + " words today. Keep your streak going!",
                            NotificationType.INFORMATION).notify(null);
        }
    }

    public void recordAgain(String id) {
        WordProgress progress = progress(id);
        progress.wrongCount++;
        progress.lastSeenAt = System.currentTimeMillis();
        progress.intervalDays = 0;
        progress.easeFactor = Math.max(1.3, progress.easeFactor - 0.2);
        progress.dueAt = progress.lastSeenAt + 10 * 60_000L;
    }

    public boolean toggleFavorite(String id) {
        WordProgress progress = progress(id);
        progress.favorite = !progress.favorite;
        return progress.favorite;
    }

    public boolean togglePinned(String id) {
        data.pinnedWord = id.equals(data.pinnedWord) ? "" : id;
        return !data.pinnedWord.isBlank();
    }

    public void snoozeMinutes(int minutes) {
        data.snoozedUntil = System.currentTimeMillis() + minutes * 60_000L;
    }

    public boolean isSnoozed() { return data.snoozedUntil > System.currentTimeMillis(); }

    public void hideToday(String id) {
        touchDay();
        data.hiddenToday.add(id);
    }

    public boolean isHiddenToday(String id) {
        touchDay();
        return data.hiddenToday.contains(id);
    }

    public String dailyWord(java.util.List<io.github.mucahidcinar.waitlingo.vocabulary.WordEntry> words) {
        String today = LocalDate.now().toString();
        if (!today.equals(data.dailyWordDate) || words.stream().noneMatch(word -> word.id().equals(data.dailyWord))) {
            data.dailyWordDate = today;
            data.dailyWord = words.get(Math.floorMod(today.hashCode(), words.size())).id();
        }
        return data.dailyWord;
    }

    private void touchDay() {
        LocalDate today = LocalDate.now();
        String todayText = today.toString();
        if (!todayText.equals(data.hiddenDate)) {
            data.hiddenDate = todayText;
            data.hiddenToday.clear();
        }
        if (todayText.equals(data.lastPracticeDate)) return;
        if (today.minusDays(1).toString().equals(data.lastPracticeDate)) data.streak++;
        else data.streak = 1;
        data.lastPracticeDate = todayText;
        data.todaySeen = 0;
        data.todayKnown = 0;
    }

    private void touchWeek() {
        LocalDate today = LocalDate.now();
        WeekFields fields = WeekFields.ISO;
        String key = today.getYear() + "-" + today.get(fields.weekOfWeekBasedYear());
        if (!key.equals(data.weekKey)) {
            data.weekKey = key;
            data.weekSeen = 0;
            data.weekKnown = 0;
        }
    }
}
