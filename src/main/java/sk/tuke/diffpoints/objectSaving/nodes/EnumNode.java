package sk.tuke.diffpoints.objectSaving.nodes;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.sun.jdi.Type;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.ui.UiColours;

public class EnumNode extends ObjectNode {

    public EnumNode(String name, String value, long id, Type type) {
        super(name, value, id, type);
    }


    @Override
    public void appendToText(SimpleColoredComponent text) {
        text.append(getValue(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.ADDRESS_VALUE.colour));
        text.append(" " + getStringRepresentationOfEnum(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.VALUE.colour));
    }

    private String getStringRepresentationOfEnum() {
        for (TreeNode child : getChildren())
            if (child.getName().equals("name"))
                return child.getValue();

        return "UNKNOWN ENUM VALUE";
    }
}
