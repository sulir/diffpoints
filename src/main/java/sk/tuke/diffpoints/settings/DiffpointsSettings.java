package sk.tuke.diffpoints.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.APP)
@State(name = "DiffpointsSettings", storages = @Storage("diffpoints.xml"))
public final class DiffpointsSettings implements PersistentStateComponent<DiffpointsSettings.SettingsState> {

    public static final class SettingsState {
        public boolean highlightOnlyRightColumn;
    }

    private SettingsState state = new SettingsState();

    public static DiffpointsSettings getInstance() {
        return ApplicationManager.getApplication().getService(DiffpointsSettings.class);
    }

    public boolean isHighlightOnlyRightColumn() {
        return state.highlightOnlyRightColumn;
    }

    public void setHighlightOnlyRightColumn(boolean highlightOnlyRightColumn) {
        state.highlightOnlyRightColumn = highlightOnlyRightColumn;
    }

    @Override
    public @Nullable DiffpointsSettings.SettingsState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull DiffpointsSettings.SettingsState state) {
        this.state = state;
    }
}
