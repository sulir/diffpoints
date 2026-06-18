package sk.tuke.diffpoints.ui;

import com.intellij.openapi.ui.Splitter;

import javax.swing.*;
import java.util.List;

public class SplitterBuilder {

    public static JComponent buildNestedSplitter(List<? extends JComponent> components) {
        return buildRecursive(components); // false = horizontal
    }

    public static JComponent buildRecursive(List<? extends JComponent> components) {
        if (components == null || components.isEmpty()) {
            return new JPanel(); // fallback
        }

        if (components.size() == 1) {
            return components.get(0); // no need for a splitter
        }

        if (components.size() == 2) {
            return createSplitter(components.get(0), components.get(1), 0.5f);
        }
        int count = components.size();

        JComponent first = components.get(0);
        List<? extends JComponent> rest = components.subList(1, count);

        float proportion = 1.0f / count;


        JComponent restNested = buildRecursive(rest);
        return createSplitter(first, restNested, proportion);
    }

    private static Splitter createSplitter(JComponent first, JComponent second, float proportion) {
        Splitter splitter = new Splitter(false, proportion); // horizontal layout
        splitter.setFirstComponent(first);
        splitter.setSecondComponent(second);
        splitter.setDividerWidth(6);
        splitter.setOpaque(true);


        return splitter;
    }
}
