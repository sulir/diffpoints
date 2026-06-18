package sk.tuke.diffpoints.objectSaving.nodes;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.sun.jdi.Type;
import sk.tuke.diffpoints.objectSaving.AlignableNode;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.ui.UiColours;

public class StringNode extends TreeNode implements AlignableNode {

    public StringNode(String name, String value, Type type) {
        super(name, value, type);
    }


    @Override
    public TreeNode createCopy(ReferenceNode referenceNode) {
        StringNode newNode = new StringNode(getName(), getRawValue(), getType());
        newNode.setParent(referenceNode.getParent());
        newNode.setDiffState(this.getDiffstate());
        return newNode;
    }

    @Override
    public String getValue() {
        return "\"" + super.getValue() + "\"";
    }

    public String getValue(String quotes) {
        if (getRawValue() == null)
            return "null";
        return quotes + super.getValue() + quotes;
    }

    @Override
    public void appendToText(SimpleColoredComponent text) {
        text.append(getValue(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.STRING_VALUE.colour));
    }

    @Override
    public String alignmentKey() {
        return getName();
    }
}
