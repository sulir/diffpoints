package sk.tuke.diffpoints.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public final class DiffpointsConfigurable implements SearchableConfigurable {

    private JBCheckBox highlightOnlyRightColumnCheckBox;
    private JPanel panel;

    @Override
    public @NotNull String getId() {
        return "sk.tuke.diffpoints.settings.DiffpointsConfigurable";
    }

    @Override
    public String getDisplayName() {
        return "Diffpoints";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            highlightOnlyRightColumnCheckBox = new JBCheckBox("Highlight only right column");
            panel = new JPanel(new BorderLayout());
            panel.setBorder(JBUI.Borders.empty(8));
            panel.add(highlightOnlyRightColumnCheckBox, BorderLayout.NORTH);
        }
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return highlightOnlyRightColumnCheckBox != null
                && highlightOnlyRightColumnCheckBox.isSelected()
                != DiffpointsSettings.getInstance().isHighlightOnlyRightColumn();
    }

    @Override
    public void apply() {
        if (highlightOnlyRightColumnCheckBox == null) {
            return;
        }

        DiffpointsSettings settings = DiffpointsSettings.getInstance();
        settings.setHighlightOnlyRightColumn(highlightOnlyRightColumnCheckBox.isSelected());
        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(DiffpointsSettingsListener.TOPIC)
                .settingsChanged();
    }

    @Override
    public void reset() {
        if (highlightOnlyRightColumnCheckBox != null) {
            highlightOnlyRightColumnCheckBox.setSelected(
                    DiffpointsSettings.getInstance().isHighlightOnlyRightColumn());
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        highlightOnlyRightColumnCheckBox = null;
    }
}
