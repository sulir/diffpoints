package sk.tuke.diffpoints.objectSaving.nodes;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.sun.jdi.Type;
import sk.tuke.diffpoints.objectSaving.AlignableNode;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.ui.UiColours;

import java.util.ArrayList;
import java.util.List;

public class ObjectNode extends TreeNode implements AlignableNode {

    private List<TreeNode> children;
    private boolean isExpanded;
    private boolean isCyclic;


    public ObjectNode(String name, String value, long id, Type type) {
        super(name, value, type, id);
        this.children = new ArrayList<>();
        this.isExpanded = false;
        this.isCyclic = false;
    }

    @Override
    public int getSize(boolean includeChildren) {
        if (!isExpanded)
            return 1;
        if (!includeChildren)
            return getChildren().size() + 1;

        int size = 1; // counting itself
        for (TreeNode child : children) {
            if (child instanceof ObjectNode objectNode) {
                size += objectNode.getSize(true);
            } else {
                size += 1;
            }
        }
        return size;
    }

    public void collapseAllObjects() {
        setExpanded(false);
        for (TreeNode child : getChildren()) {
            if (child instanceof ObjectNode objectChild) {
                objectChild.collapseAllObjects();
            }
        }
    }

    @Override
    public ObjectNode createCopy(ReferenceNode referenceNode) {
        ObjectNode newNode = new ObjectNode(referenceNode.getName(), getRawValue(), getId(), getType());
        newNode.setParent(referenceNode.getParent());
        newNode.setDiffState(this.getDiffstate());
        for (TreeNode child : children) {
            if (child instanceof NullNode nullNode)
                newNode.addChild(nullNode.createCopy(referenceNode));
            else if (child instanceof ObjectNode childNode)
                newNode.addChild(new ReferenceNode(childNode.getId(), newNode, childNode.getValue(), childNode.getName()));
            else
                newNode.addChild(child.createCopy(referenceNode));
        }
        return newNode;
    }

    @Override
    public String getValue() {
        String value = super.getValue();
        return "{" + value + "@" + getId() + "}";
    }

    @Override
    public void appendToText(SimpleColoredComponent text) {
        text.append(getValue(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.ADDRESS_VALUE.colour));

        text.append(" " + childrenToString(true), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.VALUE.colour));
    }

    public String childrenToString(boolean quotes) {
        if (getRawValue() == null)
            return "null";
        StringBuilder output = new StringBuilder();
        if (quotes)
            output.append("\"");

        output.append(getRawValue()).append("{");
        for (TreeNode child : children) {
            if (child instanceof ReferenceNode || (child instanceof ObjectNode objectNode && objectNode.getId() == this.getId()))
                continue;
            output.append(child.getName()).append("=");
            if (child instanceof StringNode stringNode)
                output.append(stringNode.getValue("'"));
            else if (child instanceof ObjectNode objectNode)
                output.append(objectNode.childrenToString(false));
            else
                output.append(child.getValue());
            output.append(", ");
        }
        output.delete(output.length() - 2, output.length()).append("}");

        if (quotes)
            output.append("\"");
        return output.toString();
    }

    public void addChild(TreeNode child) {
        children.add(child);
        child.setParent(this);
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public void toggleExpanded() {
        isExpanded = !isExpanded;
    }

    @Override
    public List<TreeNode> getChildren() {
        return children;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    @Override
    public String alignmentKey() {
        return getId() + "";
    }

    @Override
    public void setChildren(List<TreeNode> children) {
        this.children = children;
        for (TreeNode child : children) {
            child.setParent(this);
        }
    }

    public void setCyclic(boolean cyclic) {
        isCyclic = cyclic;
    }

    public boolean isCyclic() {
        return isCyclic;
    }

}
