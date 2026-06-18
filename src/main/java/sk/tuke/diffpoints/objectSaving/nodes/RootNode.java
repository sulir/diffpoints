package sk.tuke.diffpoints.objectSaving.nodes;

import sk.tuke.diffpoints.breakpoints.properties.DiffpointInfo;

import java.util.HashMap;
import java.util.Map;

public class RootNode extends ObjectNode {

    private Map<Long, ObjectNode> visitedObjects;
    private final int hitNumber;
    private final int position;
    private final String diffpointId;
    private final String diffpointName;
    private DiffpointInfo<?> diffpointInfo;

    public RootNode(int hitNumber, int position, String id, String diffpointName) {
        super("root", null, -2, null);
        this.visitedObjects = new HashMap<>();
        this.hitNumber = hitNumber;
        this.position = position;
        this.diffpointId = id;
        this.diffpointName = diffpointName;

    }

    public RootNode(int hitNumber) {
        super("root", null, -2, null);
        this.visitedObjects = new HashMap<>();
        this.hitNumber = hitNumber;
        this.position = -1;
        this.diffpointId = null;
        this.diffpointName = null;
    }

    public RootNode() {
        this(-1);
    }

    public String getDiffpointName() {
        return diffpointName;
    }

    public String getDiffpointId() {
        return diffpointId;
    }

    public int getPosition() {
        return position;
    }

    public boolean isGroupDiffpoint() {
        return position != -1;
    }

    public Map<Long, ObjectNode> getVisitedObjects() {
        return visitedObjects;
    }

    public int getHitNumber() {
        return hitNumber;
    }

    public void setDiffpointInfo(DiffpointInfo<?> diffpointInfo) {
        this.diffpointInfo = diffpointInfo;
    }

    public DiffpointInfo<?> getDiffpointInfo() {
        return diffpointInfo;
    }

    public boolean alreadyVisitedObject(long id) {
        return getVisitedObjects().containsKey(id);
    }

    public void setVisitedObjects(Map<Long, ObjectNode> visitedObjects) {
        this.visitedObjects = visitedObjects;
    }
}
