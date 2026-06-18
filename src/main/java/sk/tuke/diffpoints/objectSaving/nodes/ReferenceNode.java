package sk.tuke.diffpoints.objectSaving.nodes;

import sk.tuke.diffpoints.objectSaving.TreeNode;

public class ReferenceNode extends TreeNode {

    private final long referencedId;

    public ReferenceNode(long referencedId, TreeNode parentNode, String value, String name) {
        super(name, value, null);
        this.referencedId = referencedId;
        this.setParent(parentNode);
    }

    @Override
    public TreeNode createCopy(ReferenceNode referenceNode) {
        ReferenceNode newNode = new ReferenceNode(referencedId, this.getParent(), referenceNode.getValue(), getName());
        newNode.setParent(referenceNode.getParent());
        return newNode;
    }

    public long getReferencedId() {
        return referencedId;
    }

    @Override
    public String toString() {
        return "ReferenceNode{" +
                "referencedId=" + referencedId +
                ", name='" + getName() + '\'' +
                ", value='" + getValue() + '\'' +
                '}';
    }

}
