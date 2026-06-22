package sk.tuke.diffpoints.breakpoints;

import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;
import sk.tuke.diffpoints.breakpoints.properties.DiffpointSaveMode;
import sk.tuke.diffpoints.breakpoints.properties.LineDiffpointProperties;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class LineDiffpointBreakpointType extends JavaLineBreakpointType {

    private static final String DISPLAY_NAME = "Line Diffpoints";
    private static final String GENERAL_DESCRIPTION = "Line Diffpoint";

    public static final Icon ICON = IconLoader.getIcon("/icons/LineDiffpointIcon.svg", LineDiffpointBreakpointType.class);

    public LineDiffpointBreakpointType() {
        super("line-diffpoint-type", "Line Diffpoint");
    }

    @Override
    public @NotNull String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected @NotNull String getGeneralDescription(@NotNull XLineBreakpointType<JavaLineBreakpointProperties>.XLineBreakpointVariant variant) {
        return GENERAL_DESCRIPTION;
    }

    @Override
    public @NotNull String getGeneralDescription(@NotNull XLineBreakpoint<JavaLineBreakpointProperties> breakpoint) {
        return GENERAL_DESCRIPTION;
    }

    @Override
    public @NotNull Icon getEnabledIcon() {
        return ICON;
    }

    @Override
    public @Nullable XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaLineBreakpointProperties>>
    createCustomPropertiesPanel(@NotNull Project project) {

        return new XBreakpointCustomPropertiesPanel<>() {

            private JPanel panel;

            private JBTextField nameField;
            private JCheckBox pauseCheckbox;

            private JComboBox<DiffpointSaveMode> saveModeCombo;
            private JSpinner circularLimitSpinner;
            private JBTextField iterationField;

            private JPanel circularPanel;
            private JPanel selectedIterationsPanel;
            private JLabel infoLabel;

            @Override
            public @NotNull JComponent getComponent() {
                if (panel == null) {

                    nameField = new JBTextField();
                    nameField.getEmptyText().setText("Optional");

                    pauseCheckbox = new JCheckBox("Pause execution on every hit");

                    saveModeCombo = new ComboBox<>(DiffpointSaveMode.values());

                    circularLimitSpinner = new JSpinner(
                            new SpinnerNumberModel(3, 2, 25, 1)
                    );

                    iterationField = new JBTextField();
                    iterationField.getEmptyText().setText("Example: 1, 3, 4, 5");

                    saveModeCombo.addActionListener(e -> updateModeUI());

                    circularPanel = FormBuilder.createFormBuilder()
                            .addLabeledComponent("Max states to save:", circularLimitSpinner)
                            .getPanel();

                    selectedIterationsPanel = FormBuilder.createFormBuilder()
                            .addLabeledComponent("Iterations to save:", iterationField)
                            .getPanel();

                    JPanel spacer = new JPanel();
                    spacer.setOpaque(false);
                    spacer.setPreferredSize(new Dimension(0, 36));

                    infoLabel = new JLabel("");

                    panel = FormBuilder.createFormBuilder()
                            .addLabeledComponent("Diffpoint name:", nameField)
                            .addSeparator()
                            .addLabeledComponent("Save behavior:", saveModeCombo)
                            .addComponent((JComponent) Box.createVerticalStrut(4))
                            .addComponent(infoLabel)
                            .addComponent((JComponent) Box.createVerticalStrut(4))
                            .addComponent(circularPanel)
                            .addComponent(selectedIterationsPanel)
                            .addComponent((JComponent) Box.createVerticalStrut(4))
                            .addComponent(pauseCheckbox)
                            .addComponent(spacer)
                            .getPanel();
                    updateModeUI();
                }

                return panel;
            }

            private void updateModeUI() {
                DiffpointSaveMode mode = (DiffpointSaveMode) saveModeCombo.getSelectedItem();
                if (mode == null)
                    return;
                switch (mode) {
                    case APPEND -> infoLabel.setText("Saves state on every diffpoint hit.");
                    case CIRCULAR -> infoLabel.setText("Saves only the last N states, overwriting older ones.");
                    case COMPARE_FIRST -> infoLabel.setText("Each new diffpoint state will be compared against the first saved state.");
                    case SELECTED_ITERATIONS -> infoLabel.setText("Only saves variables for specified hit numbers.");
                }

                circularPanel.setVisible(mode == DiffpointSaveMode.CIRCULAR);
                selectedIterationsPanel.setVisible(mode == DiffpointSaveMode.SELECTED_ITERATIONS);
                pauseCheckbox.setVisible(mode == DiffpointSaveMode.APPEND || mode == DiffpointSaveMode.CIRCULAR || mode == DiffpointSaveMode.SELECTED_ITERATIONS);

                boolean isDebugging = isDebugging(project);
                nameField.setEnabled(!isDebugging);
                pauseCheckbox.setEnabled(!isDebugging);
                saveModeCombo.setEnabled(!isDebugging);
                circularLimitSpinner.setEnabled(!isDebugging);
                iterationField.setEnabled(!isDebugging);

                panel.revalidate();
                panel.repaint();
            }


            @Override
            public void loadFrom(@NotNull XLineBreakpoint<JavaLineBreakpointProperties> breakpoint) {
                if (breakpoint.getProperties() instanceof LineDiffpointProperties props) {

                    nameField.setText(props.diffpointName);
                    pauseCheckbox.setSelected(props.shouldPause);

                    saveModeCombo.setSelectedItem(props.saveMode);
                    circularLimitSpinner.setValue(props.circularLimit);
                    iterationField.setText(props.selectedIterations);

                    updateModeUI();
                }
            }

            @Override
            public void saveTo(@NotNull XLineBreakpoint<JavaLineBreakpointProperties> breakpoint) {
                if (breakpoint.getProperties() instanceof LineDiffpointProperties props) {
                    props.diffpointName = nameField.getText().trim();
                    props.shouldPause = pauseCheckbox.isSelected();
                    props.saveMode = (DiffpointSaveMode) saveModeCombo.getSelectedItem();
                    props.circularLimit = (int) circularLimitSpinner.getValue();
                    props.selectedIterations = iterationField.getText().trim();
                }
            }
        };
    }

    private static boolean isDebugging(@NotNull Project project) {
        return XDebuggerManager.getInstance(project).getCurrentSession() != null;
    }

    @Override
    public @Nullable JavaLineBreakpointProperties createProperties() {
        LineDiffpointProperties properties = new LineDiffpointProperties();
        properties.diffpointId = UUID.randomUUID().toString();
        properties.shouldPause = true;

        return properties;
    }

    @Override
    public @NotNull JavaLineBreakpointProperties createBreakpointProperties(@NotNull VirtualFile file, int line) {
        LineDiffpointProperties properties = new LineDiffpointProperties();
        properties.diffpointName = "";
        properties.diffpointId = UUID.randomUUID().toString();
        properties.shouldPause = true;

        return properties;
    }

    @Override
    public XSourcePosition getSourcePosition(@NotNull XBreakpoint<JavaLineBreakpointProperties> breakpoint) {
        return super.getSourcePosition(breakpoint);
    }

    @Override
    public @NotNull Breakpoint<JavaLineBreakpointProperties> createJavaBreakpoint(
            @NotNull Project project, @NotNull XBreakpoint<JavaLineBreakpointProperties> breakpoint) {
        return new DiffpointLineBreakpoint(project, breakpoint, ICON);
    }

}