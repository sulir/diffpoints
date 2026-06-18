package sk.tuke.diffpoints.ui;

import sk.tuke.diffpoints.objectSaving.ObjectNodeUtils;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.objectSaving.nodes.MapNode;
import sk.tuke.diffpoints.objectSaving.nodes.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjectTreeSyncManager {

    private final List<ObjectTreePanel> panels = new ArrayList<>();
    private boolean suppress = false;
    private final List<TreeNode> data;
    private final boolean forceCompare;
    private Map<String, List<TreeNode>> nodesByPath;
    private Map<TreeNode, String> nodeToKeyCache;

    public ObjectTreeSyncManager(List<TreeNode> data, boolean forceCompare) {
        this.data = data;
        this.forceCompare = forceCompare;
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
        this.nodeToKeyCache = new java.util.IdentityHashMap<>();
        for (Map.Entry<String, List<TreeNode>> entry : this.nodesByPath.entrySet()) {
            String key = entry.getKey();
            for (TreeNode n : entry.getValue()) {
                this.nodeToKeyCache.put(n, key);
            }
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

        long compareStop = System.nanoTime();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Comparing values took: %.3f ms | Memory overhead: %.3f MB%n",
                (compareStop - compareStart) / 1_000_000.0,
                (memAfter - memBefore) / (1024.0 * 1024.0));
    }

    public boolean areNodesCompatible() {
        return ObjectNodeUtils.areNodesComparable(data);
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

