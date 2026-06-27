package sk.tuke.diffpoints;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerManagerListener;
import org.jetbrains.annotations.NotNull;
import sk.tuke.diffpoints.breakpoints.properties.DiffpointInfo;
import sk.tuke.diffpoints.breakpoints.properties.GroupDiffpointProperties;
import sk.tuke.diffpoints.breakpoints.properties.LineDiffpointProperties;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.objectSaving.nodes.RootNode;
import sk.tuke.diffpoints.settings.DiffpointsSettingsListener;
import sk.tuke.diffpoints.service.CompareListener;
import sk.tuke.diffpoints.service.DiffpointListener;
import sk.tuke.diffpoints.ui.CompareWindow;
import sk.tuke.diffpoints.ui.ObjectTreePanel;
import sk.tuke.diffpoints.ui.ObjectTreeSyncManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory {

    private JComponent noDataComponent;
    private Project project;
    private Map<String, Integer> diffpointIdToTabIndex = new HashMap<>();
    private final Map<Integer, ObjectTreeSyncManager> syncManagersByTabIndex = new HashMap<>();
    private CompareWindow compareWindow;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.compareWindow = new CompareWindow(project, toolWindow, this);
        JPanel panel = new JPanel(new BorderLayout());
        noDataComponent = getNoValuesScreen();
        panel.add(noDataComponent, BorderLayout.CENTER);
        JTabbedPane tabbedPane = new JBTabbedPane();
        JButton comparePanelButton = getComparePanelButton();
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomPanel.add(comparePanelButton);

        // java.util.List<TreeNode> data = new ArrayList<>(); // for testing
        // for (int i = 0; i < 3; i++)
        // data.add(DummyData.getDummyObject(i));
        //
        // treeComponents = getOtherTreeSplitter(data);
        // //panel.add(getNoValuesScreen(), BorderLayout.CENTER);
        //
        // for (int i = 0; i < 2; i++)
        // tabbedPane.addTab(i+ "@" , getOtherTreeSplitter(data));
        // panel.add(tabbedPane);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        MessageBusConnection connection = project.getMessageBus().connect();
        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(toolWindow.getDisposable())
                .subscribe(DiffpointsSettingsListener.TOPIC, (DiffpointsSettingsListener) this::rebuildTrees);

        connection.subscribe(XDebuggerManager.TOPIC, new XDebuggerManagerListener() {
            @Override
            public void processStarted(@NotNull XDebugProcess debugProcess) {
                project.getMessageBus()
                        .syncPublisher(CompareListener.TOPIC)
                        .removeAll();
                panel.remove(noDataComponent);
                panel.remove(tabbedPane);
                panel.remove(bottomPanel);
                tabbedPane.removeAll();
                panel.setLayout(new BorderLayout());
                panel.add(tabbedPane, BorderLayout.CENTER);
                panel.add(bottomPanel, BorderLayout.SOUTH);
                panel.revalidate();
                panel.repaint();
                diffpointIdToTabIndex = new HashMap<>();
                syncManagersByTabIndex.clear();

            }

            @Override
            public void processStopped(@NotNull XDebugProcess debugProcess) {

            }
        });
        project.getMessageBus()
                .connect(toolWindow.getDisposable())
                .subscribe(DiffpointListener.TOPIC, new DiffpointListener() {
                    @Override
                    public void lineDiffpointHit(DiffpointInfo<LineDiffpointProperties> diffpointInfo) {
                        Integer index = diffpointIdToTabIndex.get(diffpointInfo.getId());
                        List<DiffpointInfo<?>> infoList = new ArrayList<>();
                        infoList.add(diffpointInfo);
                        if (index == null) {
                            diffpointIdToTabIndex.put(diffpointInfo.getId(), tabbedPane.getTabCount());
                            tabbedPane.addTab(diffpointInfo.getName(),
                                    getOtherTreeSplitter(infoList, tabbedPane, tabbedPane.getTabCount()));
                        } else {
                            tabbedPane.setComponentAt(index, getOtherTreeSplitter(infoList, tabbedPane, index));
                        }
                    }

                    @Override
                    public void groupDiffpointHit(List<DiffpointInfo<GroupDiffpointProperties>> diffpointInfo) {
                        String groupName = diffpointInfo.get(0).getGroupName();
                        Integer index = diffpointIdToTabIndex.get(groupName);
                        List<DiffpointInfo<?>> infoList = new ArrayList<>(diffpointInfo);
                        if (index == null) {
                            diffpointIdToTabIndex.put(groupName, tabbedPane.getTabCount());
                            tabbedPane.addTab(groupName,
                                    getOtherTreeSplitter(infoList, tabbedPane, tabbedPane.getTabCount()));
                        } else {
                            tabbedPane.setComponentAt(index, getOtherTreeSplitter(infoList, tabbedPane, index));
                        }

                    }
                });

    }

    private JButton getComparePanelButton() {
        JButton comparePanelButton = new JButton("Show comparison panel");
        comparePanelButton.addActionListener(e -> {
            System.out.println("Compare button clicked");
            this.compareWindow.showCompareWindow();
        });
        return comparePanelButton;
    }

    private JComponent getNoValuesScreen() {
        if (noDataComponent != null)
            return noDataComponent;

        JBLabel label = new JBLabel("No data.");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        noDataComponent = label;
        return noDataComponent;
    }

    private JComponent wrapWithIndexAndCloseButton(JComponent scrollablePanel, List<TreeNode> data, int index,
            JTabbedPane tabbedPane, int tabIndex, List<DiffpointInfo<?>> diffpointInfos) {
        if (tabbedPane == null)
            return null;
        JPanel wrapper = new JPanel(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        RootNode rootNode = (RootNode) data.get(index);
        JLabel indexLabel = new JLabel("Iteration: " + (rootNode.getHitNumber()));
        if (rootNode.isGroupDiffpoint()) {
            indexLabel.setText(rootNode.getDiffpointName());
        }
        indexLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        topBar.add(indexLabel, BorderLayout.WEST);

        if (!rootNode.isGroupDiffpoint()) {
            JButton closeButton = new JButton("X");
            closeButton.setMargin(JBUI.insets(0, 4));
            closeButton.setToolTipText("Remove this iteration");
            closeButton.setFocusable(false);
            Dimension prefSize = new Dimension(28, 28);
            closeButton.setMaximumSize(prefSize);
            closeButton.setPreferredSize(prefSize);
            closeButton.setMinimumSize(prefSize);
            closeButton.addActionListener(e -> {
                data.remove(index);
                tabbedPane.setComponentAt(tabIndex, getOtherTreeSplitter(diffpointInfos, tabbedPane, tabIndex));
            });

            topBar.add(closeButton, BorderLayout.EAST);
        } else {
            JSpinner circularLimitSpinner = getjSpinner(rootNode, tabbedPane, tabIndex, diffpointInfos);
            topBar.add(circularLimitSpinner, BorderLayout.EAST);
        }
        topBar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, JBColor.border()));

        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(scrollablePanel, BorderLayout.CENTER);

        wrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, JBColor.border()));
        return wrapper;
    }

    private @NotNull JSpinner getjSpinner(RootNode rootNode, JTabbedPane tabbedPane, int tabIndex,
            List<DiffpointInfo<?>> diffpointInfos) {
        JSpinner circularLimitSpinner = new JSpinner(
                new SpinnerNumberModel((rootNode.getDiffpointInfo().getIterationSelected() + 1), 1,
                        rootNode.getDiffpointInfo().getDataSize(), 1));
        Dimension prefSize = new Dimension(64, 28);
        circularLimitSpinner.setMaximumSize(prefSize);
        circularLimitSpinner.setPreferredSize(prefSize);
        circularLimitSpinner.setMinimumSize(prefSize);
        circularLimitSpinner.setToolTipText("Iteration");
        circularLimitSpinner.addChangeListener(e -> {
            int selectedIteration = (int) circularLimitSpinner.getValue() - 1;
            rootNode.getDiffpointInfo().setIterationSelected(selectedIteration);
            rootNode.getDiffpointInfo().disableFollowingIterations();
            tabbedPane.setComponentAt(tabIndex, getOtherTreeSplitter(diffpointInfos, tabbedPane, tabIndex));
        });
        return circularLimitSpinner;
    }

    private List<TreeNode> getDataFromDiffpoints(List<DiffpointInfo<?>> diffpointInfos) {
        if (diffpointInfos.get(0).getProperties() instanceof LineDiffpointProperties) {
            return diffpointInfos.get(0).getData();
        } else {
            List<TreeNode> combinedData = new ArrayList<>();
            for (DiffpointInfo<?> info : diffpointInfos) {
                if (info.getRootNodeAt(info.getIterationSelected()) != null) {
                    combinedData.add(info.getRootNodeAt(info.getIterationSelected()));
                }
            }

            return combinedData;
        }
    }

    private JComponent getOtherTreeSplitter(List<DiffpointInfo<?>> diffpointInfos, JTabbedPane tabbedPane,
            int tabIndex) {
        if (diffpointInfos == null || diffpointInfos.isEmpty()) {
            JPanel emptyPanel = new JPanel(new BorderLayout());
            JBLabel label = new JBLabel("No data.");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            emptyPanel.add(label, BorderLayout.CENTER);
            return emptyPanel;
        }
        List<TreeNode> data = getDataFromDiffpoints(diffpointInfos);

        java.util.List<JComponent> panes = new ArrayList<>();
        ObjectTreeSyncManager syncManager = new ObjectTreeSyncManager(data, false);
        syncManagersByTabIndex.put(tabIndex, syncManager);

        java.util.List<JBScrollPane> scrollPanes = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            RootNode root = (RootNode) data.get(i);
            if (root.isGroupDiffpoint()) {
                for (int j = 0; j < data.size(); j++) {
                    RootNode rootNode = (RootNode) data.get(j);
                    if (i == rootNode.getPosition() && rootNode.getPosition() != -1) {
                        addDataToView(data, tabbedPane, tabIndex, j, syncManager, scrollPanes, panes, diffpointInfos);
                        break;
                    }
                }
            } else {
                addDataToView(data, tabbedPane, tabIndex, i, syncManager, scrollPanes, panes, diffpointInfos);
            }
        }
        synchronizeScrollBars(scrollPanes);
        syncManager.syncAllNodes();

        return CompareWindow.createSplitters(panes);
    }

    public void addDataToView(List<TreeNode> data, JTabbedPane tabbedPane, int tabIndex, int i,
            ObjectTreeSyncManager syncManager, List<JBScrollPane> scrollPanes, List<JComponent> panes,
            List<DiffpointInfo<?>> diffpointInfos) {
        ObjectTreePanel treePanel = new ObjectTreePanel(data.get(i), data, true, syncManager, this.project);

        JBScrollPane scrollablePanel = new JBScrollPane(treePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JComponent removableWrapper = wrapWithIndexAndCloseButton(scrollablePanel, data, i, tabbedPane, tabIndex,
                diffpointInfos);
        panes.add(removableWrapper);

        scrollablePanel.getVerticalScrollBar().setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
        scrollPanes.add(scrollablePanel);
    }

    public static void synchronizeScrollBars(java.util.List<JBScrollPane> scrollPanes) {
        for (JBScrollPane source : scrollPanes) {
            BoundedRangeModel sourceModel = source.getVerticalScrollBar().getModel();

            sourceModel.addChangeListener(e -> {
                int value = sourceModel.getValue();
                for (JBScrollPane target : scrollPanes) {
                    if (target != source) {
                        target.getVerticalScrollBar().setValue(value);
                    }
                }
            });
            BoundedRangeModel sourceModel2 = source.getHorizontalScrollBar().getModel();
            ;
            sourceModel2.addChangeListener(e -> {
                int value = sourceModel2.getValue();
                for (JBScrollPane target : scrollPanes) {
                    if (target != source) {
                        target.getHorizontalScrollBar().setValue(value);
                    }
                }
            });
        }
    }

    public void rebuildTrees() {
        syncManagersByTabIndex.values().forEach(ObjectTreeSyncManager::syncAllNodes);
    }
}
