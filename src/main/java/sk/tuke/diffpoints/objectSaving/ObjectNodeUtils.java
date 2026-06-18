package sk.tuke.diffpoints.objectSaving;

import sk.tuke.diffpoints.DiffState;
import sk.tuke.diffpoints.objectSaving.nodes.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObjectNodeUtils {

    public static String findNodeGroupKey(TreeNode node, Map<String, List<TreeNode>> nodesByPath) {
        for (Map.Entry<String, List<TreeNode>> entry : nodesByPath.entrySet()) {
            if (entry.getValue().contains(node)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void replaceNodeInGroup(TreeNode target, TreeNode replacement,
            Map<String, List<TreeNode>> nodesByPath) {
        String groupKey = ObjectNodeUtils.findNodeGroupKey(target, nodesByPath);
        if (groupKey != null) {
            int index = nodesByPath.get(groupKey).indexOf(target);
            nodesByPath.get(groupKey).set(index, replacement);
        }
    }

    private static String pathKeyName(List<NodeIdentifier> path) {
        return path.stream().map(NodeIdentifier::name).collect(Collectors.joining("/"));
    }

    private static String pathKeyId(List<NodeIdentifier> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            NodeIdentifier nodeId = path.get(i);
            if (i > 0) {
                sb.append(nodeId.name()).append("/");
                continue;
            }

            if (nodeId.id().equals("-1"))
                sb.append(nodeId.name()).append("/");
            else
                sb.append(nodeId.id()).append("/");
        }

        return sb.toString();
    }

    private static void collectNodes(TreeNode node, Map<String, List<TreeNode>> byPath,
            Map<TreeNode, String> topGroupKey, boolean isGroupDiff, boolean forceCompare) {
        if (node instanceof EmptyNode)
            return;

        if (forceCompare) {
            List<NodeIdentifier> paths = node.getPathFromRoot();
            if (!(node instanceof RootNode)) {
                // TreeNode topMostParent = node.getTopMostParentNotRoot();
                paths.set(0, new NodeIdentifier("-1", "this is whatever"));
            }
            String key = pathKeyName(paths);
            byPath.computeIfAbsent(key, k -> new ArrayList<>()).add(node);

            if (node instanceof ObjectNode objNode) {
                for (TreeNode child : objNode.getChildren()) {
                    collectNodes(child, byPath, topGroupKey, false, true);
                }
            }

            return;
        }

        if (!isGroupDiff) {
            String key = pathKeyName(node.getPathFromRoot());

            byPath.computeIfAbsent(key, k -> new ArrayList<>()).add(node);

            if (node instanceof ObjectNode objNode) {
                for (TreeNode child : objNode.getChildren()) {
                    collectNodes(child, byPath, topGroupKey, false, false);
                }
            }
        } else {
            List<NodeIdentifier> paths = node.getPathFromRoot();

            if (!(node instanceof RootNode)) {
                TreeNode topMostParent = node.getTopMostParentNotRoot();
                String label = topGroupKey.getOrDefault(
                        topMostParent,
                        topMostParent.getName() + " " +
                                topMostParent.getId() + " " +
                                topMostParent.getTypeName());
                paths.set(0, new NodeIdentifier("-1", label));
            }

            String key = pathKeyName(paths);
            byPath.computeIfAbsent(key, k -> new ArrayList<>()).add(node);

            if (node instanceof ObjectNode objNode) {
                for (TreeNode child : objNode.getChildren()) {
                    collectNodes(child, byPath, topGroupKey, true, false);
                }
            }
        }
    }

    public static void replaceReferenceNodes(List<TreeNode> roots) {
        for (TreeNode root : roots) {
            for (TreeNode child : root.getChildren()) {
                if (child instanceof ReferenceNode referenceNode) {
                    RootNode rootNode = (RootNode) root;
                    ObjectNode original = rootNode.getVisitedObjects().get(referenceNode.getReferencedId());
                    ObjectNode newNode = original.createCopy(referenceNode);
                    rootNode.getChildren().set(rootNode.getChildren().indexOf(child), newNode);
                }
            }
        }
    }

    record TopParentInfo(String id, String name, String type, int treeIndex, TreeNode node) {
    }

    record Group(List<TopParentInfo> members, Set<Integer> treeIndices) {
    }

    private static TopParentInfo topInfo(TreeNode node, int treeIndex) {
        TreeNode top = node.getTopMostParentNotRoot();
        return new TopParentInfo(
                String.valueOf(top.getId()),
                top.getName(),
                top.getTypeName(),
                treeIndex,
                top);
    }

    private static void applyRule(List<TopParentInfo> all, Set<TopParentInfo> used, List<Group> groups,
            Function<TopParentInfo, String> keyExtractor, boolean allowMultiplePerKey) {

        Map<String, List<TopParentInfo>> grouped = all.stream()
                .filter(i -> !used.contains(i))
                .collect(Collectors.groupingBy(keyExtractor));

        for (List<TopParentInfo> bucket : grouped.values()) {

            List<TopParentInfo> remaining = bucket.stream()
                    .filter(i -> !used.contains(i))
                    .collect(Collectors.toCollection(ArrayList::new));

            do {
                List<TopParentInfo> members = new ArrayList<>();
                Set<Integer> trees = new HashSet<>();

                Iterator<TopParentInfo> it = remaining.iterator();
                while (it.hasNext()) {
                    TopParentInfo info = it.next();

                    if (trees.contains(info.treeIndex()))
                        continue;

                    members.add(info);
                    trees.add(info.treeIndex());
                    it.remove();
                }

                if (members.size() > 1) {
                    used.addAll(members);
                    groups.add(new Group(members, trees));
                } else {
                    break;
                }

            } while (allowMultiplePerKey && !remaining.isEmpty());
        }
    }

    private static void applyMultiKeyRule(List<TopParentInfo> all, Set<TopParentInfo> used, List<Group> groups,
            Function<TopParentInfo, List<String>> multiKeyExtractor, boolean allowMultiplePerKey) {
        Map<String, List<TopParentInfo>> grouped = new HashMap<>();
        for (TopParentInfo info : all) {
            if (used.contains(info))
                continue;
            List<String> keys = multiKeyExtractor.apply(info);
            if (keys == null)
                continue;
            for (String key : keys) {
                if (key == null || key.isEmpty())
                    continue;
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(info);
            }
        }

        List<Map.Entry<String, List<TopParentInfo>>> sortedEntries = new ArrayList<>(grouped.entrySet());
        sortedEntries.sort((e1, e2) -> {
            int sizeDiff = Integer.compare(e2.getValue().size(), e1.getValue().size());
            if (sizeDiff != 0)
                return sizeDiff;
            return e1.getKey().compareTo(e2.getKey());
        });

        for (Map.Entry<String, List<TopParentInfo>> entry : sortedEntries) {
            List<TopParentInfo> bucket = entry.getValue();
            List<TopParentInfo> remaining = bucket.stream()
                    .filter(i -> !used.contains(i))
                    .collect(Collectors.toCollection(ArrayList::new));

            do {
                List<TopParentInfo> members = new ArrayList<>();
                Set<Integer> trees = new HashSet<>();

                Iterator<TopParentInfo> it = remaining.iterator();
                while (it.hasNext()) {
                    TopParentInfo info = it.next();

                    if (trees.contains(info.treeIndex()))
                        continue;

                    members.add(info);
                    trees.add(info.treeIndex());
                    it.remove();
                }

                if (members.size() > 1) {
                    used.addAll(members);
                    groups.add(new Group(members, trees));
                } else {
                    break;
                }

            } while (allowMultiplePerKey && !remaining.isEmpty());
        }
    }

    private static List<Group> buildGroups(List<List<TopParentInfo>> perTreeInfos) {
        List<Group> groups = new ArrayList<>();
        Set<TopParentInfo> used = new HashSet<>();

        List<TopParentInfo> all = perTreeInfos.stream()
                .flatMap(List::stream)
                .toList();

        // RULE 1: id (only valid ids)
        applyRule(
                all.stream().filter(i -> !i.id().equals("-1")).toList(),
                used,
                groups,
                TopParentInfo::id,
                false);

        // RULE 2: name + type
        applyRule(
                all,
                used,
                groups,
                i -> i.name() + "|" + i.type(),
                false);

        // RULE 3: type
        applyRule(
                all,
                used,
                groups,
                TopParentInfo::type,
                true);

        // RULE 4: inheritance (superclass / interfaces)
        applyMultiKeyRule(
                all,
                used,
                groups,
                info -> {
                    List<String> keys = new ArrayList<>();
                    if (info.type() != null)
                        keys.add(info.type());
                    if (info.node().getSuperclassName() != null)
                        keys.add(info.node().getSuperclassName());
                    if (info.node().getImplementedInterfaces() != null)
                        keys.addAll(info.node().getImplementedInterfaces());
                    return keys;
                },
                true);

        // RULE 5: name
        applyRule(
                all,
                used,
                groups,
                TopParentInfo::name,
                true);

        // leftovers
        for (TopParentInfo info : all) {
            if (!used.contains(info)) {
                groups.add(new Group(
                        List.of(info),
                        Set.of(info.treeIndex())));
            }
        }

        return groups;
    }

    public static void computeAllDiffs(List<TreeNode> roots, Map<String, List<TreeNode>> nodesByPath,
            boolean forceCompare) {
        if (roots.isEmpty())
            return;

        if (forceCompare) {
            compareAny(roots, nodesByPath);
            return;
        }

        applyDiffs(roots, nodesByPath);
        propagateDiffs(roots);

    }

    private static void compareAny(List<TreeNode> roots, Map<String, List<TreeNode>> nodesByPath) {
        if (areNodesComparable(roots)) {
            applyDiffs(roots, nodesByPath);
            propagateDiffs(roots);
        } else {
            resetDiffs(roots);
        }
    }

    public static boolean areNodesComparable(List<TreeNode> roots) {
        System.out.println("Comparing nodes for compatibility:");
        for (int i = 0; i < roots.size(); i++) {
            for (int j = i + 1; j < roots.size(); j++) {
                TreeNode node1 = roots.get(i).getChildren().get(0);
                TreeNode node2 = roots.get(j).getChildren().get(0);
                if (node1 instanceof NullNode || node2 instanceof NullNode) {
                    continue;
                }
                System.out.println(node1.getType().name() + " vs " + node2.getType().name());
                if (!node1.getType().name().equals(node2.getType().name())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void resetDiffs(List<TreeNode> roots) {
        for (TreeNode root : roots) {
            root.setDiffState(DiffState.UNCHANGED);
            if (root instanceof ObjectNode objNode) {
                for (TreeNode child : objNode.getChildren()) {
                    resetDiffs(Collections.singletonList(child));
                }
            }
        }
    }

    static Map<TreeNode, String> buildGroupKeyIndex(List<Group> groups) {
        Map<TreeNode, String> result = new HashMap<>();
        for (Group g : groups) {
            // choose canonical label for the group
            TopParentInfo rep = g.members().get(0);
            String groupLabel = rep.name() + " " + rep.id() + " " + rep.type();
            for (TopParentInfo m : g.members()) {
                result.put(m.node(), groupLabel);
            }
        }
        return result;
    }

    private static void collectTopInfos(TreeNode node, int treeIndex,
            List<TopParentInfo> out) {
        if (!(node instanceof RootNode)) {
            out.add(topInfo(node, treeIndex));
        }
        if (node instanceof ObjectNode obj) {
            for (TreeNode child : obj.getChildren()) {
                collectTopInfos(child, treeIndex, out);
            }
        }
    }

    public static Map<String, List<TreeNode>> getNodesByPath(List<TreeNode> roots, boolean isGroupDiff,
            boolean forceCompare) {
        Map<String, List<TreeNode>> nodesByPath = new HashMap<>();
        if (forceCompare) {
            for (TreeNode root : roots) {
                collectNodes(root, nodesByPath, null, false, true);
            }
            return nodesByPath;
        }
        if (isGroupDiff) {
            List<List<TopParentInfo>> perTreeInfos = new ArrayList<>();
            for (int i = 0; i < roots.size(); i++) {
                List<TopParentInfo> list = new ArrayList<>();
                collectTopInfos(roots.get(i), i, list);
                perTreeInfos.add(list);
            }
            List<Group> groups = buildGroups(perTreeInfos);
            Map<TreeNode, String> topGroupKey = buildGroupKeyIndex(groups);

            for (TreeNode root : roots) {
                collectNodes(root, nodesByPath, topGroupKey, true, forceCompare);
            }

        } else {
            for (TreeNode root : roots) {
                collectNodes(root, nodesByPath, null, false, forceCompare);
            }
        }
        return nodesByPath;
    }

    public static void applyDiffs(List<TreeNode> roots, Map<String, List<TreeNode>> nodesByPath) {
        // System.out.println("Collected paths:");
        // for (String path : nodesByPath.keySet()) {
        // System.out.print("'" + path + "' : ");
        // for (TreeNode node : nodesByPath.get(path)) {
        // System.out.print("'"+node.getName() + "', ");
        // }
        // System.out.println();
        // }
        // System.out.println("-----------");

        for (List<TreeNode> sameNodes : nodesByPath.values()) {
            applyDiffForSamePath(sameNodes);
        }
    }

    private static void applyDiffForSamePath(List<TreeNode> sameNodes) {
        List<String> values = sameNodes.stream()
                .map(TreeNode::getRawValue)
                .toList();

        DiffState diffState = detectDifferences(values);

        for (TreeNode node : sameNodes) {
            node.setDiffState(diffState);
        }
    }

    public static void propagateDiffs(List<TreeNode> roots) {
        for (TreeNode root : roots) {
            propagateFromChildren(root);
        }
    }

    private static void propagateFromChildren(TreeNode node) {
        if (node instanceof ObjectNode objNode) {

            boolean childHasDiff = false;

            for (TreeNode child : objNode.getChildren()) {
                propagateFromChildren(child);

                if (child.getDiffstate() != DiffState.UNCHANGED) {
                    childHasDiff = true;
                }
            }

            if (childHasDiff && node.getDiffstate() == DiffState.UNCHANGED) {
                node.setDiffState(DiffState.CHANGED);
            }
        }
    }

    private static DiffState detectDifferences(List<String> orderedValues) {
        if (orderedValues.isEmpty())
            return DiffState.UNCHANGED;

        String first = orderedValues.get(0);
        String last = orderedValues.get(orderedValues.size() - 1);

        boolean anyNonNullBeforeLast = orderedValues
                .subList(0, orderedValues.size() - 1)
                .stream()
                .anyMatch(Objects::nonNull);

        boolean allEqual = orderedValues.stream().distinct().count() == 1;

        if (allEqual) {
            return DiffState.UNCHANGED;
        }

        if (first == null && last != null) {

            return DiffState.ADDED;
        }

        if (last == null && anyNonNullBeforeLast) {
            return DiffState.REMOVED;
        }

        return DiffState.CHANGED;
    }

    private static LinkedHashSet<String> collectAllFirstLayerGroupKeys(List<TreeNode> roots,
            Map<TreeNode, String> nodeToKeyCache) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();

        for (TreeNode root : roots) {
            for (TreeNode child : root.getChildren()) {
                if (child instanceof EmptyNode) {
                    continue;
                }
                String key = nodeToKeyCache.get(child);
                keys.add(key);
            }
        }
        return keys; // insertion order preserved
    }

    public static void alignFirstLayer(List<TreeNode> roots, Map<TreeNode, String> nodeToKeyCache) {
        LinkedHashSet<String> allKeys = collectAllFirstLayerGroupKeys(roots, nodeToKeyCache);

        for (TreeNode root : roots) {
            Map<String, TreeNode> existing = root.getChildren().stream()
                    .filter(c -> !(c instanceof EmptyNode))
                    .collect(Collectors.toMap(
                            nodeToKeyCache::get,
                            c -> c,
                            (a, b) -> a,
                            LinkedHashMap::new));

            List<TreeNode> aligned = new ArrayList<>();
            for (String key : allKeys) {
                TreeNode child = existing.get(key);
                aligned.add(child != null ? child : new EmptyNode(key));
            }
            root.setChildren(aligned);
        }
    }
}