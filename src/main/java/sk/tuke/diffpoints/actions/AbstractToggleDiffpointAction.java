package sk.tuke.diffpoints.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

abstract class AbstractToggleDiffpointAction<T extends XLineBreakpointType<? extends JavaLineBreakpointProperties>>
        extends DumbAwareAction {

    private final String addText;
    private final String removeText;
    private final Class<T> breakpointTypeClass;

    protected AbstractToggleDiffpointAction(@NotNull String diffpointName, @NotNull Class<T> breakpointTypeClass) {
        super("Add " + diffpointName);
        this.addText = "Add " + diffpointName;
        this.removeText = "Remove " + diffpointName;
        this.breakpointTypeClass = breakpointTypeClass;
    }

    @Override
    public final @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public final void update(@NotNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        GutterContext context = getContext(event);
        T breakpointType = getBreakpointType();

        if (context == null || breakpointType == null || !breakpointType.canPutAt(context.file(), context.line(), context.project())) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        boolean exists = XDebuggerManager.getInstance(context.project())
                .getBreakpointManager()
                .getBreakpoints(castBreakpointType(breakpointType))
                .stream()
                .anyMatch(breakpoint -> breakpoint.getLine() == context.line()
                        && context.file().getUrl().equals(breakpoint.getFileUrl()));

        presentation.setText(exists ? removeText : addText);
        presentation.setEnabledAndVisible(true);
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent event) {
        GutterContext context = getContext(event);
        T breakpointType = getBreakpointType();
        if (context == null || breakpointType == null) {
            return;
        }

        XDebuggerUtil.getInstance().toggleLineBreakpoint(
                context.project(),
                castBreakpointType(breakpointType),
                context.file(),
                context.line(),
                false
        );
    }

    @SuppressWarnings("unchecked")
    private static XLineBreakpointType<JavaLineBreakpointProperties> castBreakpointType(
            @NotNull XLineBreakpointType<? extends JavaLineBreakpointProperties> breakpointType) {
        return (XLineBreakpointType<JavaLineBreakpointProperties>) breakpointType;
    }

    private @Nullable T getBreakpointType() {
        return XDebuggerUtil.getInstance().findBreakpointType(breakpointTypeClass);
    }

    private static @Nullable GutterContext getContext(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        Integer line = event.getData(EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR);
        VirtualFile file = editor == null ? event.getData(CommonDataKeys.VIRTUAL_FILE) : editor.getVirtualFile();

        if (project == null || file == null || line == null || line < 0) {
            return null;
        }

        return new GutterContext(project, file, line);
    }

    private record GutterContext(@NotNull Project project, @NotNull VirtualFile file, int line) {
    }
}
