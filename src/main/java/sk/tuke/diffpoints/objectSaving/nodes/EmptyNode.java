package sk.tuke.diffpoints.objectSaving.nodes;

import sk.tuke.diffpoints.objectSaving.TreeNode;

public class EmptyNode extends TreeNode {


    public EmptyNode(String name) {
        super(name, null, null, -1);
        try {
            setId(Long.parseLong(name));
        } catch (NumberFormatException e) {
            setId(-1);
        }
    }

    @Override
    public TreeNode createCopy(ReferenceNode referenceNode) {
        return new EmptyNode(getName());
    }
}
