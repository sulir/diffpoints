package sk.tuke.diffpoints.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.tuke.diffpoints.DiffState;
import sk.tuke.diffpoints.objectSaving.ObjectNodeUtils;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.objectSaving.nodes.*;
import sk.tuke.diffpoints.service.CompareListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;

public class ObjectTreePanel extends JPanel implements Scrollable {

    private static final int ROW_HEIGHT = 22;

    private final TreeNode root;
    private final ObjectTreeSyncManager syncManager;
    private final List<TreeNode> data;
    private ObjectColoredComponent selectedRow;
    private final Project project;
    private final boolean mainWindow;
    private int rowCount;
    private List<VirtualRow> virtualRows = new ArrayList<>();
    private int lastStartIndex = -1, lastEndIndex = -1;

    private static class VirtualRow {
        TreeNode node;
        int indent;
        boolean isNullFiller;

        VirtualRow(TreeNode node, int indent, boolean isNullFiller) {
            this.node = node;
            this.indent = indent;
            this.isNullFiller = isNullFiller;
        }
    }

    public ObjectTreePanel(TreeNode root, List<TreeNode> data, boolean mainWindow, ObjectTreeSyncManager syncManager,
            Project project) {
        this.rowCount = 0;
        this.project = project;
        this.root = root;
        this.mainWindow = mainWindow;
        this.data = data;
        this.syncManager = syncManager;
        this.syncManager.register(this);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (getParent() instanceof JViewport viewport) {
            viewport.addChangeListener(e -> updateVisibleRows());
        }
    }

    private void renderNode(TreeNode node, int indent, boolean parentExpanded) {
        if (node instanceof RootNode rootNode) { // dont show root
            for (TreeNode child : rootNode.getChildren())
                renderNode(child, indent, false);
            return;
        }
        if (node instanceof ReferenceNode referenceNode) {
            System.out.println("Hot did we get here? " + node.getName());
            return;
        }

        TreeNode parent = node.getParent();
        while (!(parent instanceof RootNode)) {
            if (parent.getDiffstate() == DiffState.REMOVED) {
                node.setDiffState(DiffState.REMOVED);
                break;
            }
            if (parent.getDiffstate() == DiffState.ADDED) {
                node.setDiffState(DiffState.ADDED);
                break;
            }
            parent = parent.getParent();
        }

        virtualRows.add(new VirtualRow(node, indent, false));
        rowCount++;

        if (node instanceof MapNode mapNode) {
            if (mapNode.isExpanded()) {
                for (TreeNode element : mapNode.getChildren()) {
                    // MapNode.Element newElement = (MapNode.Element) element;
                    renderNode(element, indent + 1, true);
                    // renderNode(element.value(), indent + 1, true);
                }
            }
        } else if (node instanceof ObjectNode objectNode) {
            if (objectNode.isExpanded()) {
                for (TreeNode child : objectNode.getChildren()) {
                    if (child instanceof ReferenceNode referenceNode) {
                        RootNode rootNode = (RootNode) root;
                        ObjectNode original = rootNode.getVisitedObjects().get(referenceNode.getReferencedId());
                        ObjectNode newNode = original.createCopy(referenceNode);
                        objectNode.getChildren().set(objectNode.getChildren().indexOf(child), newNode);
                        ObjectNodeUtils.replaceNodeInGroup(child, newNode, syncManager.getNodesByPath());

                        if (objectNode.getValue().equals(newNode.getValue()))
                            newNode.setCyclic(true);
                        syncManager.recomputeAllDiffs();
                        renderNode(newNode, indent + 1, true);
                    } else {
                        renderNode(child, indent + 1, true);
                    }
                }
            }
        }
        fillRowsUnderNullObjects(node, indent);
    }

    private JPopupMenu createPopUpPanel(ObjectColoredComponent rowPanel, TreeNode node) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (mainWindow) {
            JMenuItem customCompare = getCompareItem(node);
            popupMenu.add(customCompare);
        }
        if (node instanceof ObjectNode objectNode) {
            popupMenu.addSeparator();
            JMenuItem collapseExpand = new JMenuItem(objectNode.isExpanded() ? "  Collapse" : "  Expand");
            collapseExpand.addActionListener(e -> expandNode(node));
            popupMenu.add(collapseExpand);

            JMenuItem collapseAll = new JMenuItem("  Collapse all");
            collapseAll.addActionListener(e -> {
                objectNode.collapseAllObjects();
                syncManager.onNodeToggled(objectNode, false);
            });
            popupMenu.add(collapseAll);
        }
        return popupMenu;
    }

    private @NotNull JMenuItem getCompareItem(TreeNode node) {
        JMenuItem customCompare = new JMenuItem(
                !node.isInComparisonMode() ? "  + add to compare" : "  - remove from compare");
        customCompare.addActionListener(e -> {
            node.setInComparisonMode(!node.isInComparisonMode());
            if (node.isInComparisonMode())
                this.project.getMessageBus()
                        .syncPublisher(CompareListener.TOPIC)
                        .nodeAdded(node);
            else
                this.project.getMessageBus()
                        .syncPublisher(CompareListener.TOPIC)
                        .nodeRemoved(node);
            syncManager.syncAllNodes();
        });
        return customCompare;
    }

    private void addCollapseButtonToRow(TreeNode node, ObjectColoredComponent rowPanel) {
        Dimension rowDim = new Dimension(24, ROW_HEIGHT);

        rowPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        expandNode(node);
                    } else {
                        selectNode(rowPanel);
                    }
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    selectNode(rowPanel);
                    createPopUpPanel(rowPanel, node).show(rowPanel, e.getX(), e.getY());
                }
            }
        });

        if (node instanceof ObjectNode objectNode && !objectNode.getChildren().isEmpty()) {
            JButton toggle = new JButton();
            toggle.setIcon(objectNode.isExpanded() ? AllIcons.General.ArrowDown : AllIcons.General.ArrowRight);
            toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
            toggle.setMargin(JBUI.emptyInsets());
            toggle.setFocusable(false);
            toggle.setBorder(null);
            toggle.setContentAreaFilled(false);

            toggle.setMinimumSize(rowDim);
            toggle.setPreferredSize(rowDim);
            toggle.setMaximumSize(rowDim);
            toggle.addActionListener(e -> {
                expandNode(objectNode);
            });

            rowPanel.add(toggle);
            if (objectNode.isCyclic()) {
                JLabel cycleLabel = new JLabel(Icons.CYCLE);
                cycleLabel.setToolTipText("Cyclic reference");
                cycleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                cycleLabel.setBorder(null);
                cycleLabel.setOpaque(false);
                cycleLabel.setMinimumSize(rowDim);
                cycleLabel.setPreferredSize(rowDim);
                cycleLabel.setMaximumSize(rowDim);
                rowPanel.add(cycleLabel);
            }

        } else {
            rowPanel.add(Box.createRigidArea(rowDim)); // space for toggle button
        }
        addWarningIfNotCompatible(node, rowPanel, rowDim);
        if (node.isInComparisonMode() && mainWindow) {
            JLabel compareLabel = new JLabel(Icons.MAG_LENS);
            compareLabel.setToolTipText("Included in comparison");
            compareLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            compareLabel.setBorder(null);
            compareLabel.setOpaque(false);
            compareLabel.setMinimumSize(rowDim);
            compareLabel.setPreferredSize(rowDim);
            compareLabel.setMaximumSize(rowDim);
            rowPanel.add(compareLabel);
        }
    }

    private void addWarningIfNotCompatible(TreeNode node, ObjectColoredComponent rowPanel, Dimension rowDim) {
        String nodeGroupKey = syncManager.getNodeGroupKey(node);
        if (nodeGroupKey != null && syncManager.getNodesByPath().get(nodeGroupKey).size() > 1) {
            List<TreeNode> roots = syncManager.getNodesByPath().get(nodeGroupKey);
            boolean compatible = true;
            for (int i = 0; i < roots.size(); i++) {
                for (int j = i + 1; j < roots.size(); j++) {
                    TreeNode node1 = roots.get(i);
                    TreeNode node2 = roots.get(j);
                    if (node1 instanceof NullNode || node2 instanceof NullNode || node2 instanceof ReferenceNode) {
                        continue;
                    }
                    if (!node1.getTypeName().equals(node2.getTypeName())) {
                        compatible = false;
                        // System.out.println("%" + node1 + "% vs %" + node2 + "%");
                        break;
                    }
                }
            }
            if (!compatible) {
                JLabel compatibleLabel = new JLabel(Icons.WARNING);
                compatibleLabel.setToolTipText("Variables of different types, cannot be reliably compared");
                compatibleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                compatibleLabel.setBorder(null);
                compatibleLabel.setOpaque(false);
                compatibleLabel.setMinimumSize(rowDim);
                compatibleLabel.setPreferredSize(rowDim);
                compatibleLabel.setMaximumSize(rowDim);
                rowPanel.add(compatibleLabel);
            }
        }
    }

    private void selectNode(ObjectColoredComponent row) {
        if (selectedRow != null && selectedRow != row) {
            selectedRow.setSelected(false);
        }
        // select new
        selectedRow = row;
        row.setSelected(true);
    }

    public RootNode getRootNode() {
        return (RootNode) root;
    }

    private void fillRowsUnderNullObjects(TreeNode node, int indent) {
        // if (!(node instanceof NullNode) && !(node instanceof EmptyNode))
        // return;

        boolean includeChildren = node instanceof NullNode;
        String nodeGroupKey = syncManager.getNodeGroupKey(node);
        int maxSize = 0;
        if (nodeGroupKey != null) {
            for (TreeNode foundNode : syncManager.getNodesByPath().get(nodeGroupKey)) {
                int foundSize = foundNode.getSize(includeChildren);
                if (foundSize > maxSize) {
                    maxSize = foundSize;
                }
            }
        }

        // for (TreeNode root : data) {
        // TreeNode obj = findNodeByPath(root, node.getPathFromRoot(),
        // getRootNode().isGroupDiffpoint());
        // if (obj instanceof ObjectNode objectNode) {
        // int foundSize = objectNode.getSize(includeChildren);
        // //objectNode.getChildren().size();
        // if (foundSize > maxSize)
        // maxSize = foundSize;
        // }
        // }

        int rowsToAdd = maxSize - node.getSize(includeChildren);
        for (int i = 0; i < rowsToAdd; i++) {
            virtualRows.add(new VirtualRow(node, indent, true));
            rowCount++;
        }

    }

    private void addTextToRow(TreeNode node, ObjectColoredComponent rowPanel) {
        if (node instanceof EmptyNode)
            return;
        SimpleColoredComponent text = new SimpleColoredComponent();
        text.setOpaque(false); // non-opaque background, so diff background will be visible

        String identifier;
        String sign;
        if (node.getParent() instanceof MapNode.Element element)
            identifier = (element.key() == node) ? "key" : "value";
        else
            identifier = node.getName();

        text.append(identifier,
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.VARIABLE_NAME.colour));
        text.append(node instanceof MapNode.Element ? " -> " : " = ",
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.EQUAL_SIGN.colour));

        if (node instanceof NullNode)
            text.append("null", new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UiColours.VALUE.colour));
        else
            node.appendToText(text);

        rowPanel.add(text);
        revalidate();
        repaint();
    }

    private @NotNull ObjectColoredComponent getColoredRowBasedOnDiff(@Nullable TreeNode node) {
        if (node == null)
            return new ObjectColoredComponent(null);

        ObjectColoredComponent rowPanel;
        if (node.getDiffstate() == DiffState.CHANGED)
            rowPanel = new ObjectColoredComponent(UiColours.MODIFIED_DIFF.colour);
        else if (node.getDiffstate() == DiffState.ADDED)
            rowPanel = new ObjectColoredComponent(UiColours.CREATED_DIFF.colour);
        else if (node.getDiffstate() == DiffState.REMOVED)
            rowPanel = new ObjectColoredComponent(UiColours.REMOVED_DIFF.colour);
        else
            rowPanel = new ObjectColoredComponent(null);

        return rowPanel;
    }

    private void expandNode(TreeNode node) {
        if (!(node instanceof ObjectNode objectNode))
            return;

        objectNode.toggleExpanded();
        if (syncManager != null) {
            syncManager.onNodeToggled(objectNode, objectNode.isExpanded());
        }
    }

    public void rebuildTree() {
        virtualRows.clear();
        lastStartIndex = -1;
        lastEndIndex = -1;
        rowCount = 0;
        renderNode(root, 0, false);

        updateVisibleRows();
    }

    private void updateVisibleRows() {
        if (!(getParent() instanceof JViewport viewport)) {
            System.out.println("HERE");
            removeAll();
            for (VirtualRow vRow : virtualRows) {
                if (vRow.isNullFiller) {
                    add(createEmptyRow(vRow.node, vRow.indent));
                } else {
                    add(createRealRow(vRow.node, vRow.indent));
                }
            }
            revalidate();
            repaint();
            return;
        }

        Rectangle viewRect = viewport.getViewRect();
        int startIndex = Math.max(0, viewRect.y / ROW_HEIGHT);
        int endIndex = Math.min(virtualRows.size() - 1, (viewRect.y + viewRect.height) / ROW_HEIGHT + 1);

        if (viewRect.height == 0) {
            endIndex = Math.min(virtualRows.size() - 1, startIndex + 20);
        }

        if (startIndex == lastStartIndex && endIndex == lastEndIndex) {
            return;
        }

        lastStartIndex = startIndex;
        lastEndIndex = endIndex;

        removeAll();
        if (startIndex > 0) {
            add(Box.createVerticalStrut(startIndex * ROW_HEIGHT));
        }

        for (int i = startIndex; i <= endIndex; i++) {
            VirtualRow vRow = virtualRows.get(i);
            if (vRow.isNullFiller) {
                add(createEmptyRow(vRow.node, vRow.indent));
            } else {
                add(createRealRow(vRow.node, vRow.indent));
            }
        }

        if (endIndex < virtualRows.size() - 1) {
            add(Box.createVerticalStrut((virtualRows.size() - 1 - endIndex) * ROW_HEIGHT));
        }

        revalidate();
        repaint();
    }

    private ObjectColoredComponent createRealRow(TreeNode node, int indent) {
        ObjectColoredComponent rowPanel = getColoredRowBasedOnDiff(node);
        rowPanel.setOpaque(false);
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        rowPanel.add(Box.createHorizontalStrut((indent * 22)));
        addCollapseButtonToRow(node, rowPanel);
        addTextToRow(node, rowPanel);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        rowPanel.setMinimumSize(new Dimension(0, ROW_HEIGHT));
        rowPanel.setPreferredSize(new Dimension(0, ROW_HEIGHT));
        return rowPanel;
    }

    private ObjectColoredComponent createEmptyRow(TreeNode node, int indent) {
        ObjectColoredComponent emptyRow = getColoredRowBasedOnDiff(node);
        if (node.getDiffstate() == DiffState.CHANGED)
            emptyRow = getColoredRowBasedOnDiff(null);

        emptyRow.setOpaque(false);
        emptyRow.setLayout(new BoxLayout(emptyRow, BoxLayout.X_AXIS));
        emptyRow.add(Box.createHorizontalStrut((indent * 20)));
        emptyRow.add(Box.createRigidArea(new Dimension(24, ROW_HEIGHT)));
        emptyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        emptyRow.setMinimumSize(new Dimension(0, ROW_HEIGHT));
        emptyRow.setPreferredSize(new Dimension(0, ROW_HEIGHT));
        emptyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        return emptyRow;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return ROW_HEIGHT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height - ROW_HEIGHT;
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 800; // or calculated from content
        return new Dimension(width, syncManager.getMaxRowCount() * ROW_HEIGHT + 10);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public int getRowCount() {
        return rowCount;
    }
}