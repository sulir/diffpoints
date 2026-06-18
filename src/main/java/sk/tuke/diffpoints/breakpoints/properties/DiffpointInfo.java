package sk.tuke.diffpoints.breakpoints.properties;

import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.objectSaving.nodes.RootNode;

import java.util.ArrayList;
import java.util.List;

public class DiffpointInfo<DiffpointProperties extends JavaLineBreakpointProperties> {

    private final String filePath;
    private final String fileName;
    private final int lineNumber;
    private int hitCount;
    private List<TreeNode> data;
    private DiffpointProperties properties;
    private int iterationSelected;
    private boolean isFollowingIterations;

    public DiffpointInfo(List<TreeNode> data) {
        this("", "", -1, null);
        this.data = data;
        this.iterationSelected = 0;
        this.isFollowingIterations = true;
    }

    public DiffpointInfo(String filePath, String fileName, int lineNumber, DiffpointProperties properties) {
        this(filePath, fileName, lineNumber);
        this.properties = properties;
        this.iterationSelected = 0;
        this.isFollowingIterations = true;

    }

    public DiffpointInfo(String filePath, String fileName, int lineNumber) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.hitCount = 0;
        this.data = new ArrayList<>();
        this.properties = null;
        this.iterationSelected = 0;
        this.isFollowingIterations = true;
    }

    public RootNode getRootNodeAt(int pos) {
        if (pos >= 0 && pos < data.size()) {
            return (RootNode) data.get(pos);
        }
        return null;
    }

    public void replaceRootNodeAt(int pos, RootNode rootNode) {
        if (data.isEmpty()) {
            data.add(rootNode);
            return;
        }

        if (pos >= 0 && pos < data.size()) {
            data.set(pos, rootNode);
        }
    }

    public ArrayList<Integer> getSelectedIterations() {
        if (properties instanceof LineDiffpointProperties lineProp && lineProp.selectedIterations != null && !lineProp.selectedIterations.isEmpty()) {
            String[] parts = lineProp.selectedIterations.split(",");
            ArrayList<Integer> iterations = new ArrayList<>();
            for (String part : parts) {
                try {
                    iterations.add(Integer.parseInt(part.trim().replace(" ", "")));
                } catch (NumberFormatException e) {
                    // ignore invalid numbers
                }
            }
            return iterations;
        }
        return new ArrayList<>();
    }

    public DiffpointSaveMode getMode() {
        if (properties instanceof LineDiffpointProperties lineProp) {
            return lineProp.saveMode;
        }
        return DiffpointSaveMode.APPEND;
    }

    public String getId() {
        if (properties instanceof LineDiffpointProperties lineProp && lineProp.diffpointId != null && !lineProp.diffpointId.isEmpty()) {
            return lineProp.diffpointId;
        }
        if (properties instanceof GroupDiffpointProperties groupProp && groupProp.diffpointId != null && !groupProp.diffpointId.isEmpty()) {
            return groupProp.diffpointId;
        }
        return fileName + "@Line " + (lineNumber + 1);
    }

    public String getGroupName() {
        if (properties instanceof GroupDiffpointProperties groupProp && groupProp.groupName != null) {
            if (groupProp.groupName.isEmpty())
                return "Group 1"; // TODO
            return groupProp.groupName;
        }
        return "Group 1";
    }

    public String getName() {
        if (properties instanceof LineDiffpointProperties lineProp && lineProp.diffpointName != null && !lineProp.diffpointName.isEmpty())
            return lineProp.diffpointName;

        if (properties instanceof GroupDiffpointProperties groupProp && groupProp.diffpointName != null && !groupProp.diffpointName.isEmpty())
            return groupProp.diffpointName;

        return fileName + "@Line " + (lineNumber + 1);
    }

    public DiffpointProperties getProperties() {
        return properties;
    }

    public int getCircularLimit() {
        if (properties instanceof LineDiffpointProperties lineProp) {
            return lineProp.circularLimit;
        }
        return 5;
    }

    public boolean shouldPause() {
        if (properties instanceof LineDiffpointProperties lineProp)
            return lineProp.shouldPause;
        if (properties instanceof GroupDiffpointProperties groupProp)
            return groupProp.shouldPause;
        return true;
    }

    public int getDataSize() {
        return data.size();
    }

    public void reset() {
        this.hitCount = 0;
        this.data = new ArrayList<>();
    }


    public void addToSecondPosition(RootNode rootNode) {
        if (this.data.size() == 1) {
            this.addRootNode(rootNode);
        } else {
            this.data.remove(data.size() - 1);
            this.addRootNode(rootNode);
        }
    }

    public void addRootNode(RootNode rootNode) {
        this.data.add(rootNode);
        rootNode.setDiffpointInfo(this);
    }

    public List<TreeNode> getData() {
        return data;
    }

    public void incrementHitCount() {
        this.hitCount++;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getHitCount() {
        return hitCount;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getIterationSelected() {
        return iterationSelected;
    }

    public void setIterationSelected(int iterationSelected) {
        this.iterationSelected = iterationSelected;
    }

    public void disableFollowingIterations() {
        this.isFollowingIterations = false;
    }

    public boolean isFollowingIterations() {
        return isFollowingIterations;
    }
}
