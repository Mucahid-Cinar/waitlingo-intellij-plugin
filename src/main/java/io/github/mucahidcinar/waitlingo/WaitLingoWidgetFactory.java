package io.github.mucahidcinar.waitlingo;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class WaitLingoWidgetFactory implements StatusBarWidgetFactory {

    public static final String ID = "WaitLingo";

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return "WaitLingo";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return !project.isDisposed();
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new WaitLingoWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        if (widget instanceof Disposable) {
            Disposer.dispose((Disposable) widget);
        }
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
