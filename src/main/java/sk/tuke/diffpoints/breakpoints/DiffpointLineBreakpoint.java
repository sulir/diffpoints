package sk.tuke.diffpoints.breakpoints;

import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import javax.swing.*;

/**
 * JDI-side breakpoint used for diffpoints. The platform computes the gutter icon during a debug
 * session via {@code BreakpointWithHighlighter#calcIcon}: once the breakpoint is verified it calls
 * {@link #getVerifiedIcon(boolean)}, whose default implementation returns the stock red
 * {@code Db_verified_breakpoint} icon. We override it so a verified diffpoint keeps its own
 * (blue / green) icon instead of turning red.
 */
public class DiffpointLineBreakpoint extends LineBreakpoint<JavaLineBreakpointProperties> {

    private final Icon icon;

    public DiffpointLineBreakpoint(Project project, XBreakpoint<JavaLineBreakpointProperties> xBreakpoint, Icon icon) {
        super(project, xBreakpoint);
        this.icon = icon;
    }

    @Override
    protected Icon getVerifiedIcon(boolean isMuted) {
        return icon;
    }
}
