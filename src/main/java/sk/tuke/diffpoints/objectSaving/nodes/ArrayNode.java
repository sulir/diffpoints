package sk.tuke.diffpoints.objectSaving.nodes;

import com.sun.jdi.Type;
import sk.tuke.diffpoints.objectSaving.TreeNode;

public class ArrayNode extends ObjectNode {


    public ArrayNode(String name, String value, long address, Type type) {
        super(name, value, address, type);
    }

    @Override
    public ObjectNode createCopy(ReferenceNode referenceNode) {
        return super.createCopy(referenceNode);
    }

    @Override
    public String getValue() {
       // return "{" + getRawValue() + "[" + getChildren().size() + "]@" + getId() + "}";
        return "{" + replaceLeftMostBrackets(getRawValue()) + "@" + getId() + "}";
    }

    private String replaceLeftMostBrackets(String str) {
        int index = str.indexOf('[');
        if (index == -1) {
            return str;
        }
        return str.substring(0, index + 1) + getChildren().size() + str.substring(index + 1);
    }


    @Override
    public String childrenToString(boolean quotes) {
        if (getChildren() == null || getChildren().isEmpty())
            return "[]";
        if (getChildren().get(0) instanceof ObjectNode || getChildren().get(0) instanceof ReferenceNode)
            return "";

        StringBuilder sb = new StringBuilder("[");

        for (TreeNode child : getChildren()) {
            if (child instanceof StringNode)
                sb.append("\"").append(child.getRawValue()).append("\", ");
            else
                sb.append(child.getRawValue()).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("]");
        return sb.toString();
    }
}
