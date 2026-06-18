package sk.tuke.diffpoints.objectSaving;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.sun.jdi.Type;
import sk.tuke.diffpoints.DiffState;
import sk.tuke.diffpoints.objectSaving.nodes.ReferenceNode;
import sk.tuke.diffpoints.objectSaving.nodes.RootNode;
import sk.tuke.diffpoints.ui.UiColours;

import java.util.ArrayList;
import java.util.List;

public class TreeNode implements AlignableNode {

    private final String name;
    private final String value;
    private final Type type;
    private TreeNode parent;
    private DiffState diffState;
    private long id;
    private boolean inComparisonMode;
    private String superclassName;
    private List<String> implementedInterfaces;

    public TreeNode(String name, String value, Type type, long id) {
        this.name = name;
        this.value = value;
        this.parent = null;
        this.diffState = DiffState.UNCHANGED;
        this.type = type;
        this.id = id;
        this.inComparisonMode = false;
    }

    public TreeNode(String name, String value, Type type) {
        this.name = name;
        this.value = value;
        this.parent = null;
        this.diffState = DiffState.UNCHANGED;
        this.type = type;
        this.id = -1;
        this.inComparisonMode = false;
    }

    public TreeNode createCopy(ReferenceNode referenceNode) {
        TreeNode newNode = new TreeNode(getName(), getRawValue(), getType());
        newNode.setParent(referenceNode.getParent());
        newNode.setDiffState(this.getDiffstate());
        return newNode;
    }

    public List<NodeIdentifier> getPathFromRoot() {
        List<NodeIdentifier> paths = new ArrayList<>();
        TreeNode current = this;
        while (current != null) {
            if (current instanceof RootNode)
                break;
            paths.add(0, new NodeIdentifier(
                    String.valueOf(current.getId()),
                    current.getName()));
            current = current.getParent();
        }
        return paths;
    }

    public List<String> getNamePathFromRoot() {
        List<String> path = new ArrayList<>();
        TreeNode current = this;
        while (current != null) {
            path.add(0, current.getName());
            current = current.getParent();
        }
        return path;
    }

    public List<String> getIdPathFromRoot() {
        List<String> path = new ArrayList<>();
        TreeNode current = this;
        while (current != null) {
            path.add(0, current.getId() == -1 ? current.getName() : String.valueOf(current.getId()));
            current = current.getParent();
        }
        return path;
    }

    public void appendToText(SimpleColoredComponent text) {
        text.append(this.getValue(),
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.VALUE.colour));
    }

    public void setDiffState(DiffState diffState) {
        this.diffState = diffState;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public String getRawValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public TreeNode getParent() {
        return parent;
    }

    public TreeNode getTopMostParentNotRoot() {
        TreeNode current = this;
        TreeNode lastNonRoot = null;
        while (current != null) {
            if (current instanceof RootNode)
                break;
            lastNonRoot = current;
            current = current.getParent();
        }
        return lastNonRoot != null ? lastNonRoot : this;
    }

    public DiffState getDiffstate() {
        return diffState;
    }

    public Type getType() {
        return type;
    }

    public String getTypeName() {
        return type != null ? type.name() : "null";
    }

    public long getId() {
        return id;
    }

    public RootNode getRoot() {
        TreeNode current = this;
        while (current != null) {
            if (current instanceof RootNode)
                return (RootNode) current;
            current = current.getParent();
        }
        return null;
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "diffState=" + diffState +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", type=" + type +
                ", parent=" + parent +
                '}';
    }

    @Override
    public String alignmentKey() {
        return getName();
    }

    public List<TreeNode> getChildren() {
        return new ArrayList<>();
    }

    public void setChildren(List<TreeNode> alignedChildren) {
        return;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getSize(boolean includeChildren) {
        return 1;
    }

    public void setInComparisonMode(boolean inComparisonMode) {
        this.inComparisonMode = inComparisonMode;
    }

    public boolean isInComparisonMode() {
        return inComparisonMode;
    }

    public String getSuperclassName() {
        return superclassName;
    }

    public void setSuperclassName(String superclassName) {
        this.superclassName = superclassName;
    }

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public void addImplementedInterface(String implementedInterface) {
        if (this.implementedInterfaces == null)
            this.implementedInterfaces = new ArrayList<>();
        this.implementedInterfaces.add(implementedInterface);
    }
}
