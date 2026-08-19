package io.github.mucahidcinar.waitlingo;

import com.intellij.util.messages.Topic;

public interface WaitLingoCommands {
    Topic<WaitLingoCommands> TOPIC = Topic.create("WaitLingo commands", WaitLingoCommands.class);
    void nextWord();
}
