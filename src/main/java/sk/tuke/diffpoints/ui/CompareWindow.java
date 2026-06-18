package sk.tuke.diffpoints.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import sk.tuke.diffpoints.ToolWindowFactory;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.objectSaving.nodes.RootNode;
import sk.tuke.diffpoints.service.CompareListener;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CompareWindow {

    private JDialog comparisonDialog;
    private final ArrayList<TreeNode> nodes;
    private final JPanel panel;
    private final ToolWindowFactory toolWindowFactory;
    private final Project project;

    public CompareWindow(Project project, ToolWindow toolWindow, ToolWindowFactory toolWindowFactory) {
        this.project = project;
        this.nodes = new ArrayList<>();
        panel = new JPanel(new BorderLayout());
        this.toolWindowFactory = toolWindowFactory;
        project.getMessageBus()
                .connect(toolWindow.getDisposable())
                .subscribe(CompareListener.TOPIC, new CompareListener() {
                    @Override
                    public void nodeAdded(TreeNode node) {
                        nodes.add(node);
                        createComparisonTree();
                    }

                    @Override
                    public void nodeRemoved(TreeNode node) {
                        nodes.remove(node);
                        createComparisonTree();
                    }

                    @Override
                    public void removeAll() {
                        nodes.clear();
                        createComparisonTree();
                    }
                });
    }

    private void createComparisonTree() {
        if (comparisonDialog == null || !comparisonDialog.isVisible()) {
            return;
        }
        panel.removeAll();
        if (nodes.isEmpty()) {
            JPanel emptyPanel = new JPanel(new BorderLayout());
            JBLabel label = new JBLabel("No variables selected for detailed comparison.");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            emptyPanel.add(label, BorderLayout.CENTER);
            panel.add(emptyPanel, BorderLayout.CENTER);
            panel.revalidate();
            panel.repaint();
            return;
        }
        ArrayList<TreeNode> data = new ArrayList<>();
        for (TreeNode node : nodes) {
            RootNode rootNode = new RootNode();
            rootNode.setVisitedObjects(node.getRoot().getVisitedObjects());

            rootNode.addChild(node);
            data.add(rootNode);
        }

        java.util.List<JComponent> panes = new ArrayList<>();
        ObjectTreeSyncManager syncManager = new ObjectTreeSyncManager(data, true);
        java.util.List<JBScrollPane> scrollPanes = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            ObjectTreePanel treePanel = new ObjectTreePanel(data.get(i), data, false, syncManager, this.project);

            JPanel topBar = createRemoveButton(data, i);
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(topBar, BorderLayout.NORTH);
            JBScrollPane scrollablePanel = new JBScrollPane(treePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            wrapper.add(scrollablePanel, BorderLayout.CENTER);

            panes.add(wrapper);
            scrollablePanel.getVerticalScrollBar().setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
            scrollPanes.add(scrollablePanel);
        }

        ToolWindowFactory.synchronizeScrollBars(scrollPanes);
        syncManager.rebuildAllPanels();
        JPanel marginWrapper = createSplitters(panes);
        panel.setLayout(new BorderLayout());
        panel.add(createTopPanel(syncManager.areNodesCompatible()), BorderLayout.NORTH);
        panel.add(marginWrapper, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();

    }

    private @NotNull JPanel createRemoveButton(ArrayList<TreeNode> data, int index) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JButton closeButton = new JButton("X");
        closeButton.setMargin(JBUI.insets(0, 4));
        closeButton.setToolTipText("Remove this variable from comparison");
        closeButton.setFocusable(false);
        Dimension prefSize = new Dimension(28, 28);
        closeButton.setMaximumSize(prefSize);
        closeButton.setPreferredSize(prefSize);
        closeButton.setMinimumSize(prefSize);
        closeButton.addActionListener(e -> {
            this.nodes.remove(data.get(index).getChildren().get(0));
            data.get(index).getChildren().get(0).setInComparisonMode(false);
            toolWindowFactory.rebuildTrees();
            createComparisonTree();
        });
        JLabel removeLabel = new JLabel("Remove variable");
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.add(closeButton, BorderLayout.WEST);
        wrapper.add(removeLabel, BorderLayout.WEST);
        topBar.add(wrapper, BorderLayout.WEST);
        return topBar;
    }

    private JPanel createTopPanel(boolean compatible) {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topPanel.setBorder(JBUI.Borders.empty(12, 12, 5, 0));
        if (!compatible) {
            JLabel compareLabel = new JLabel(Icons.WARNING);
            compareLabel.setBorder(JBUI.Borders.emptyRight(12));
            compareLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            compareLabel.setBorder(null);
            compareLabel.setOpaque(false);
            topPanel.add(compareLabel);
            JBLabel label = new JBLabel("Variables are of different type, cannot safely compare them.");
            topPanel.add(label);
        }

        return topPanel;
    }

    public static JPanel createSplitters(List<JComponent> panes) {
        JComponent splitters = SplitterBuilder.buildNestedSplitter(panes);
        JPanel border = new JPanel(new BorderLayout());
        border.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, JBColor.border())); // top, left, bottom, right
        border.add(splitters, BorderLayout.CENTER);

        JPanel marginWrapper = new JPanel(new BorderLayout());
        marginWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8)); // top, left, bottom, right
        marginWrapper.add(border, BorderLayout.CENTER);
        return marginWrapper;
    }

    public void showCompareWindow() {
        if (comparisonDialog == null) {
            comparisonDialog = new JDialog((Frame) null, "Custom Comparison", false);
            comparisonDialog.setSize(600, 400);
            comparisonDialog.setLocationRelativeTo(null);

            comparisonDialog.add(panel);
        }

        comparisonDialog.setVisible(true);
        createComparisonTree();
    }


}
