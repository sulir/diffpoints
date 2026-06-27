package sk.tuke.diffpoints.ui;

import sk.tuke.diffpoints.DiffState;
import sk.tuke.diffpoints.objectSaving.ObjectNodeUtils;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.objectSaving.nodes.MapNode;
import sk.tuke.diffpoints.objectSaving.nodes.ObjectNode;
import sk.tuke.diffpoints.objectSaving.nodes.RootNode;
import sk.tuke.diffpoints.settings.DiffpointsSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ObjectTreeSyncManager {

    private final List<ObjectTreePanel> panels = new ArrayList<>();
    private boolean suppress = false;
    private final List<TreeNode> data;
    private final boolean forceCompare;
    private Map<String, List<TreeNode>> nodesByPath;
    private Map<TreeNode, String> nodeToKeyCache;
    private final Map<TreeNode, Integer> rootIndices = new IdentityHashMap<>();
    private Map<TreeNode, DiffState> displayDiffStates = new IdentityHashMap<>();

    public ObjectTreeSyncManager(List<TreeNode> data, boolean forceCompare) {
        this.data = data;
        this.forceCompare = forceCompare;
        buildRootIndices();
        ObjectNodeUtils.replaceReferenceNodes(data);
        this.nodesByPath = ObjectNodeUtils.getNodesByPath(data, data.get(0).getRoot().isGroupDiffpoint(), forceCompare);

        buildCache();

        Runtime runtime = Runtime.getRuntime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long renderStart = System.nanoTime();

        alignFirstLayer();
        long renderStop = System.nanoTime();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Caching values took: %.3f ms | Memory overhead: %.3f MB%n",
                (renderStop - renderStart) / 1_000_000.0,
                (memAfter - memBefore) / (1024.0 * 1024.0));
        System.out.println("-----------");
    }

    private void buildCache() {
        this.nodeToKeyCache = new IdentityHashMap<>();
        for (Map.Entry<String, List<TreeNode>> entry : this.nodesByPath.entrySet()) {
            String key = entry.getKey();
            for (TreeNode n : entry.getValue()) {
                this.nodeToKeyCache.put(n, key);
            }
        }
    }

    private void buildRootIndices() {
        for (int i = 0; i < data.size(); i++) {
            RootNode rootNode = (RootNode) data.get(i);
            rootIndices.put(rootNode, rootNode.isGroupDiffpoint() ? rootNode.getPosition() : i);
        }
    }

    public String getNodeGroupKey(TreeNode node) {
        if (nodeToKeyCache == null) {
            buildCache();
        }
        return nodeToKeyCache.get(node);
    }

    public int getMaxRowCount() {
        return panels.stream().mapToInt(ObjectTreePanel::getRowCount).max().orElse(0);
    }

    public void register(ObjectTreePanel panel) {
        panels.add(panel);
    }

    public void onNodeToggled(ObjectNode toggledNode, boolean newExpandedState) {
        onNodeToggled(toggledNode, newExpandedState, true);
    }

    public void onNodeToggled(ObjectNode toggledNode, boolean newExpandedState, boolean rebuildTrees) {
        if (suppress)
            return;
        suppress = true;
        String nodeGroupKey = getNodeGroupKey(toggledNode);
        if (nodeGroupKey != null) {
            nodesByPath.get(nodeGroupKey)
                    .forEach(node -> {
                        if (node instanceof ObjectNode objectNode) {
                            objectNode.setExpanded(newExpandedState);
                        }
                    });
        }

        if (rebuildTrees)
            rebuildAllPanels();

        suppress = false;
    }

    public void rebuildAllPanels() {
        Runtime runtime = Runtime.getRuntime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long renderStart = System.nanoTime();

        recomputeAllDiffs();

        for (ObjectTreePanel panel : panels)
            panel.rebuildTree();

        long renderStop = System.nanoTime();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Rendering values took: %.3f ms | Memory overhead: %.3f MB%n",
                (renderStop - renderStart) / 1_000_000.0,
                (memAfter - memBefore) / (1024.0 * 1024.0));
        System.out.println("-----------");
    }

    public void syncAllNodes() {
        for (ObjectTreePanel panel : panels)
            expandExpandedNodes(panel.getRootNode());
        rebuildAllPanels();
    }

    private void expandExpandedNodes(TreeNode sourceNode) {
        for (TreeNode node : sourceNode.getChildren()) {
            if (node instanceof ObjectNode objectNode && objectNode.isExpanded()) {
                onNodeToggled(objectNode, true, false);
                expandExpandedNodes(objectNode);
            }
        }
    }

    public void recomputeAllDiffs() {
        Runtime runtime = Runtime.getRuntime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long compareStart = System.nanoTime();

        this.nodesByPath = ObjectNodeUtils.getNodesByPath(data, data.get(0).getRoot().isGroupDiffpoint(), forceCompare);
        buildCache();
        ObjectNodeUtils.computeAllDiffs(data, nodesByPath, forceCompare);
        recomputeDisplayDiffStates();

        long compareStop = System.nanoTime();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Comparing values took: %.3f ms | Memory overhead: %.3f MB%n",
                (compareStop - compareStart) / 1_000_000.0,
                (memAfter - memBefore) / (1024.0 * 1024.0));
    }

    public boolean areNodesCompatible() {
        return ObjectNodeUtils.areNodesComparable(data);
    }

    public DiffState getDisplayDiffState(TreeNode node) {
        if (!useRightColumnHighlighting()) {
            return node.getDiffstate();
        }
        return displayDiffStates.getOrDefault(node, DiffState.UNCHANGED);
    }

    private boolean useRightColumnHighlighting() {
        return !forceCompare && DiffpointsSettings.getInstance().isHighlightOnlyRightColumn();
    }

    private void recomputeDisplayDiffStates() {
        displayDiffStates = new IdentityHashMap<>();
        if (!useRightColumnHighlighting()) {
            return;
        }

        for (List<TreeNode> sameNodes : nodesByPath.values()) {
            Map<Integer, TreeNode> nodesByColumn = new HashMap<>();
            for (TreeNode node : sameNodes) {
                Integer columnIndex = rootIndices.get(node.getRoot());
                if (columnIndex != null) {
                    nodesByColumn.put(columnIndex, node);
                }
            }

            for (Map.Entry<Integer, TreeNode> entry : nodesByColumn.entrySet()) {
                int columnIndex = entry.getKey();
                if (columnIndex == 0) {
                    continue;
                }

                DiffState pairwiseDiffState = getPairwiseDiffState(nodesByColumn.get(columnIndex - 1), entry.getValue());
                if (pairwiseDiffState != DiffState.UNCHANGED) {
                    displayDiffStates.put(entry.getValue(), pairwiseDiffState);
                }
            }
        }

        propagateDisplayDiffStates();
    }

    private DiffState getPairwiseDiffState(TreeNode leftNode, TreeNode rightNode) {
        if (leftNode == null) {
            return DiffState.ADDED;
        }
        return ObjectNodeUtils.detectDifferences(Arrays.asList(leftNode.getRawValue(), rightNode.getRawValue()));
    }

    private void propagateDisplayDiffStates() {
        for (TreeNode root : data) {
            propagateDisplayDiffStateFromChildren(root);
        }
    }

    private boolean propagateDisplayDiffStateFromChildren(TreeNode node) {
        DiffState currentDiffState = displayDiffStates.getOrDefault(node, DiffState.UNCHANGED);
        boolean subtreeHasDiff = currentDiffState != DiffState.UNCHANGED;

        if (node instanceof ObjectNode objectNode) {
            boolean childHasDiff = false;
            for (TreeNode child : objectNode.getChildren()) {
                if (propagateDisplayDiffStateFromChildren(child)) {
                    childHasDiff = true;
                }
            }

            if (childHasDiff && currentDiffState == DiffState.UNCHANGED) {
                displayDiffStates.put(node, DiffState.CHANGED);
                subtreeHasDiff = true;
            } else if (childHasDiff) {
                subtreeHasDiff = true;
            }
        }

        return subtreeHasDiff;
    }

    public void alignFirstLayer() {
        if (nodeToKeyCache == null)
            buildCache();
        ObjectNodeUtils.alignFirstLayer(data, nodeToKeyCache);
        for (List<TreeNode> nodes : nodesByPath.values()) {
            if (nodes.size() > 1 && !(nodes.get(0) instanceof MapNode.Element))
                ObjectNodeUtils.alignFirstLayer(nodes, nodeToKeyCache);
        }
    }

    public Map<String, List<TreeNode>> getNodesByPath() {
        return nodesByPath;
    }
}


// TODO:
// uvod aky je problem, preco chcemie riesit, konkretny priklad
// ako je mozne to aktualne riesit, preco niesu dost dobre
// ako riesi problem nas program
// ako my riesime problem, zhora dole, ako funguje ui o co nam slo
// naco je to dobre, ako sme testovali blizsie opisat
// zaver co by sa dalo este robit, zhrnutie celkovo prace
