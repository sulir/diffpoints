package sk.tuke.diffpoints.objectSaving.nodes;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.sun.jdi.Type;
import sk.tuke.diffpoints.ui.UiColours;

public class ListNode extends ObjectNode {

    public ListNode(String name, String value, long id, Type type) {
        super(name, value, id, type);
    }

    @Override
    public void appendToText(SimpleColoredComponent text) {
        text.append(getValue(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.ADDRESS_VALUE.colour));
        text.append("  size = " + getChildren().size(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.VALUE.colour));
    }
}
