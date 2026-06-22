package sk.tuke.diffpoints.breakpoints;

import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;
import sk.tuke.diffpoints.breakpoints.properties.GroupDiffpointProperties;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GroupDiffpointBreakpointType extends JavaLineBreakpointType {
    private static final String DISPLAY_NAME = "Group Diffpoints";
    private static final String GENERAL_DESCRIPTION = "Group Diffpoint";

    public static final Icon ICON = IconLoader.getIcon("/icons/GroupDiffpointIcon.svg", LineDiffpointBreakpointType.class);

    public GroupDiffpointBreakpointType() {
        super("group-diffpoint-type", "Group Diffpoint");
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
    public @Nullable JavaLineBreakpointProperties createProperties() {
        GroupDiffpointProperties properties = new GroupDiffpointProperties();
        properties.diffpointId = UUID.randomUUID().toString();

        return properties;
    }

    @Override
    public @NotNull JavaLineBreakpointProperties createBreakpointProperties(@NotNull VirtualFile file, int line) {
        GroupDiffpointProperties properties = new GroupDiffpointProperties();
        properties.diffpointName = "";
        properties.diffpointId = UUID.randomUUID().toString();

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

    @Override
    public @Nullable XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaLineBreakpointProperties>>
    createCustomPropertiesPanel(@NotNull Project project) {

        return new XBreakpointCustomPropertiesPanel<>() {

            private JPanel panel;

            private JBTextField nameField;
            private JCheckBox pauseCheckbox;

            private ComboBoxWithWidePopup<String> groupBox;

            @Override
            public @NotNull JComponent getComponent() {
                if (panel == null) {

                    nameField = new JBTextField();
                    nameField.getEmptyText().setText("Optional");

                    groupBox = new ComboBoxWithWidePopup<>();
                    groupBox.setEditable(true);

                    Dimension size = groupBox.getPreferredSize();
                    groupBox.setPreferredSize(new Dimension(250, size.height));

                    pauseCheckbox = new JCheckBox("Pause execution on every hit");
                    panel = FormBuilder.createFormBuilder()
                            .addLabeledComponent("Diffpoint name:", nameField)
                            .addLabeledComponent("Group:", groupBox)
                            .addSeparator()
                            .addComponent(pauseCheckbox)
                            .getPanel();
                }

                return panel;
            }

            @Override
            public void loadFrom(@NotNull XLineBreakpoint<JavaLineBreakpointProperties> breakpoint) {
                if (breakpoint.getProperties() instanceof GroupDiffpointProperties props) {
                    nameField.setText(props.diffpointName);
                    groupBox.setModel(new DefaultComboBoxModel<>(getExistingGroups(project)));
                    groupBox.setSelectedItem(props.groupName);
                    pauseCheckbox.setSelected(props.shouldPause);
                }
            }

            @Override
            public void saveTo(@NotNull XLineBreakpoint<JavaLineBreakpointProperties> breakpoint) {
                if (breakpoint.getProperties() instanceof GroupDiffpointProperties props) {
                    props.diffpointName = nameField.getText().trim();
                    props.groupName = ((String) groupBox.getEditor().getItem()).trim();
                    props.shouldPause = pauseCheckbox.isSelected();
                }
            }
        };
    }

    private String[] getExistingGroups(Project project) {
        XBreakpointManager manager = XDebuggerManager.getInstance(project).getBreakpointManager();

        Set<String> groups = new HashSet<>();

        for (XBreakpoint<?> bp : manager.getAllBreakpoints()) {
            if (bp.getProperties() instanceof GroupDiffpointProperties props) {
                if (props.groupName != null && !props.groupName.isBlank()) {
                    groups.add(props.groupName);
                }
            }
        }

        return groups.toArray(new String[0]);
    }
}
