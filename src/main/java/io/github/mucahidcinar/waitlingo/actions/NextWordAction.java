package io.github.mucahidcinar.waitlingo.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import io.github.mucahidcinar.waitlingo.WaitLingoCommands;
import org.jetbrains.annotations.NotNull;

public final class NextWordAction extends AnAction {
    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        if (event.getProject() != null) {
            event.getProject().getMessageBus().syncPublisher(WaitLingoCommands.TOPIC).nextWord();
        }
    }
}
