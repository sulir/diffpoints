package sk.tuke.diffpoints.objectSaving.nodes;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.sun.jdi.Type;
import org.jetbrains.annotations.Nullable;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.ui.UiColours;

public class MapNode extends ObjectNode {

    private Element temporaryElement;
    private boolean addingKey;

    public MapNode(String name, String value, long id, Type type) {
        super(name, value, id, type);
        this.temporaryElement = null;
        this.addingKey = true;
    }

    @Override
    public void appendToText(SimpleColoredComponent text) {
        text.append(getValue(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.ADDRESS_VALUE.colour));
        text.append("  size = " + getChildren().size(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.VALUE.colour));
    }

    @Override
    public void addChild(TreeNode child) {
        if (addingKey) {
            temporaryElement = new Element(child, null, -1);
        } else {
            if (temporaryElement != null) {
                temporaryElement = new Element(temporaryElement.key(), child, getId() + getChildren().size() + 1);
                getChildren().add(temporaryElement);
                temporaryElement.setParent(this);
                temporaryElement = null;
            }
        }
        this.addingKey = !this.addingKey;
    }

    public static class Element extends ObjectNode {

        public Element(TreeNode key, @Nullable TreeNode value, long id) {
            super(key.getName(), value != null ? value.getValue() : null, id, null);
            addChild(key);
            if (value != null)
                addChild(value);
        }

        public TreeNode key() {
            return getChildren().get(0);
        }

        public TreeNode value() {
            return getChildren().get(1);
        }

        @Override
        public void appendToText(SimpleColoredComponent text) {
            value().appendToText(text);
        }
    }


}
