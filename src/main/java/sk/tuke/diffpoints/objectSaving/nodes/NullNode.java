package sk.tuke.diffpoints.objectSaving.nodes;

public class NullNode extends ObjectNode {

    public NullNode(String name) {
        super(name, null, -1, null);
    }

    @Override
    public NullNode createCopy(ReferenceNode referenceNode) {
        NullNode newNode = new NullNode(getName());
        newNode.setParent(referenceNode.getParent());
        newNode.setDiffState(this.getDiffstate());
        return newNode;
    }

}
