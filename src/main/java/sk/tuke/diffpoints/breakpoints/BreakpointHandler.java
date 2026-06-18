package sk.tuke.diffpoints.breakpoints;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcessAdapterImpl;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.*;
import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.EventSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;
import sk.tuke.diffpoints.breakpoints.properties.DiffpointInfo;
import sk.tuke.diffpoints.breakpoints.properties.DiffpointSaveMode;
import sk.tuke.diffpoints.breakpoints.properties.GroupDiffpointProperties;
import sk.tuke.diffpoints.breakpoints.properties.LineDiffpointProperties;
import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.objectSaving.nodes.*;
import sk.tuke.diffpoints.service.DiffpointListener;

import java.util.*;

public class BreakpointHandler implements DebuggerManagerListener {

    VirtualMachine virtualMachine;
    private final Project project;
    private final Map<Class<? extends XBreakpointType<?, ?>>, List<DiffpointInfo<?>>> diffpoints;

    public BreakpointHandler(Project project) {
        this.project = project;
        this.diffpoints = new HashMap<>();
        diffpoints.put(LineDiffpointBreakpointType.class, new ArrayList<>());
        diffpoints.put(GroupDiffpointBreakpointType.class, new ArrayList<>());

        project.getMessageBus().connect().subscribe(XBreakpointListener.TOPIC,
                new XBreakpointListener<>() {
                    @Override
                    public void breakpointAdded(@NotNull XBreakpoint<?> breakpoint) {
                        addDiffpoint(breakpoint, LineDiffpointBreakpointType.class);
                        addDiffpoint(breakpoint, GroupDiffpointBreakpointType.class);
                    }

                    @Override
                    public void breakpointRemoved(@NotNull XBreakpoint<?> breakpoint) {
                        removeDiffpoint(breakpoint, LineDiffpointBreakpointType.class);
                        removeDiffpoint(breakpoint, GroupDiffpointBreakpointType.class);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private <P extends JavaLineBreakpointProperties> void addDiffpoint(XBreakpoint<?> breakpoint,
            Class<? extends XBreakpointType<?, ?>> typeClass) {
        XSourcePosition position = breakpoint.getSourcePosition();
        if (position == null || !typeClass.isInstance(breakpoint.getType()))
            return;

        P props = (P) breakpoint.getProperties();
        diffpoints.get(typeClass).add(new DiffpointInfo<>(
                position.getFile().getPath(),
                position.getFile().getName(),
                position.getLine(),
                props));
    }

    private void removeDiffpoint(XBreakpoint<?> breakpoint, Class<? extends XBreakpointType<?, ?>> typeClass) {
        XSourcePosition position = breakpoint.getSourcePosition();
        if (position == null || !typeClass.isInstance(breakpoint.getType()))
            return;

        String path = position.getFile().getPath();
        int line = position.getLine();

        diffpoints.get(typeClass).removeIf(loc -> loc.getFilePath().equals(path) && loc.getLineNumber() == line);
    }

    @Override
    public void sessionAttached(DebuggerSession session) {
        XDebugSession xSession = session.getXDebugSession();
        if (xSession == null)
            return;
        DebuggerManagerThreadImpl managerThread = session.getProcess().getManagerThread();
        managerThread.invoke(new DebuggerCommandImpl() {
            @Override
            protected void action() {
                virtualMachine = session.getProcess().getVirtualMachineProxy().getVirtualMachine();
                // System.out.println("[Diffpoints] VM attached (via listener): " +
                // virtualMachine.name());
            }
        });
    }

    private <P extends JavaLineBreakpointProperties> void loadExistingDiffpoints(Project project,
            Class<? extends XLineBreakpointType<P>> typeClass) {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();

        XLineBreakpointType<P> type = XDebuggerUtil.getInstance().findBreakpointType(typeClass);

        if (type == null)
            return;

        Collection<? extends XLineBreakpoint<P>> breakpoints = breakpointManager.getBreakpoints(type);

        List<DiffpointInfo<? extends JavaLineBreakpointProperties>> targetList = diffpoints.get(typeClass);

        if (targetList == null)
            return;

        for (XLineBreakpoint<P> breakpoint : breakpoints) {
            XSourcePosition position = breakpoint.getSourcePosition();
            if (position == null)
                continue;

            P props = breakpoint.getProperties();
            if (props == null)
                continue;

            targetList.add(
                    new DiffpointInfo<>(
                            position.getFile().getPath(),
                            position.getFile().getName(),
                            position.getLine(),
                            props));
        }
    }

    @Override
    public void sessionCreated(DebuggerSession session) {
        diffpoints.values().forEach(list -> list.forEach(DiffpointInfo::reset));

        loadExistingDiffpoints(project, LineDiffpointBreakpointType.class);
        loadExistingDiffpoints(project, GroupDiffpointBreakpointType.class);

        DebugProcessImpl debugProcessImpl = session.getProcess();
        debugProcessImpl.addDebugProcessListener(new DebugProcessAdapterImpl() {
            @Override
            public void paused(SuspendContextImpl suspendContext) {
                EventSet eventSet = suspendContext.getEventSet();
                if (eventSet == null)
                    return;

                for (var event : eventSet) {
                    Object requester = RequestManagerImpl.findRequestor(event.request());
                    if (event instanceof BreakpointEvent bp && requester instanceof Breakpoint<?> breakpoint) {
                        XBreakpoint<?> xBreakpoint = breakpoint.getXBreakpoint();
                        boolean isDiffpoint = xBreakpoint != null
                                && (xBreakpoint.getType() instanceof LineDiffpointBreakpointType
                                        || xBreakpoint.getType() instanceof GroupDiffpointBreakpointType);
                        if (isDiffpoint) {
                            boolean shouldPause = performJdiWorkOnBreakpoint(bp, debugProcessImpl,
                                    xBreakpoint.getType() instanceof LineDiffpointBreakpointType);

                            pauseProcess(suspendContext, shouldPause, debugProcessImpl);
                        }
                    }
                }
            }

            @Override
            public void processDetached(DebugProcessImpl process, boolean closedByUser) {
                // diffpoints.values().forEach(list -> list.forEach(DiffpointInfo::reset));

                debugProcessImpl.removeDebugProcessListener(this);
            }
        });
    }

    private static void pauseProcess(SuspendContextImpl suspendContext, boolean shouldPause,
            DebugProcessImpl debugProcessImpl) {
        if (!shouldPause) {
            debugProcessImpl.getManagerThread().invoke(new DebuggerCommandImpl() {
                @Override
                protected void action() {
                    suspendContext.getDebugProcess().getSuspendManager().resume(suspendContext);
                }
            });
        }
    }

    private Optional<DiffpointInfo<? extends JavaLineBreakpointProperties>> findDiffpoint(String path, int line,
            boolean isLineDiffpoint) {
        if (isLineDiffpoint) {
            List<DiffpointInfo<?>> lineDiffpoints = diffpoints.get(LineDiffpointBreakpointType.class);
            if (lineDiffpoints == null)
                return Optional.empty();

            return lineDiffpoints.stream()
                    .filter(loc -> loc.getFilePath().equals(path) &&
                            loc.getLineNumber() == line)
                    .findFirst();
        } else {
            List<DiffpointInfo<?>> groupDiffpoints = diffpoints.get(GroupDiffpointBreakpointType.class);
            if (groupDiffpoints == null)
                return Optional.empty();

            return groupDiffpoints.stream()
                    .filter(loc -> loc.getFilePath().equals(path) &&
                            loc.getLineNumber() == line)
                    .findFirst();

        }
    }

    @SuppressWarnings("unchecked")
    private boolean performJdiWorkOnBreakpoint(BreakpointEvent bpEvent, DebugProcessImpl debugProcessImpl,
            boolean isLineDiffpoint) {
        Location location = bpEvent.location();
        SourcePosition sourcePosition = debugProcessImpl.getPositionManager().getSourcePosition(location);

        if (sourcePosition == null)
            return true;

        VirtualFile file = sourcePosition.getFile().getVirtualFile();
        String path = file.getPath();
        int line = sourcePosition.getLine();
        // System.out.println(sourcePosition.getElementAt().getText() );

        Optional<DiffpointInfo<? extends JavaLineBreakpointProperties>> maybeDiffpoint = findDiffpoint(path, line,
                isLineDiffpoint);
        if (maybeDiffpoint.isEmpty()) {
            // System.out.println("[Diffpoints] No diffpoint found for " + path + "@" +
            // line);
            return true;
        }
        boolean shouldPause = true;
        DiffpointInfo<? extends JavaLineBreakpointProperties> diffpointInfo = maybeDiffpoint.get();

        try {
            ThreadReference thread = bpEvent.thread();
            if (thread.frameCount() > 0) {
                if (diffpointInfo.getProperties() instanceof LineDiffpointProperties) {
                    // System.out.println("[Diffpoints] Line diffpoint hit: " +
                    // diffpointInfo.getName());
                    shouldPause = lineDiffpointWork(thread, (DiffpointInfo<LineDiffpointProperties>) diffpointInfo);
                } else if (diffpointInfo.getProperties() instanceof GroupDiffpointProperties) {
                    shouldPause = groupDiffpointWork(thread, (DiffpointInfo<GroupDiffpointProperties>) diffpointInfo);
                    // System.out.println("[Diffpoints] Group diffpoint hit: " +
                    // diffpointInfo.getName());
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        return shouldPause;
    }

    @SuppressWarnings("unchecked")
    private List<DiffpointInfo<GroupDiffpointProperties>> getGroupDiffpointsByGroupName(String groupName) {
        List<DiffpointInfo<?>> allGroupDiffpoints = diffpoints.get(GroupDiffpointBreakpointType.class);
        List<DiffpointInfo<GroupDiffpointProperties>> result = new ArrayList<>();

        for (DiffpointInfo<?> diffpoint : allGroupDiffpoints) {
            if (!(diffpoint.getProperties() instanceof GroupDiffpointProperties))
                continue;

            if (diffpoint.getGroupName().equals(groupName))
                result.add((DiffpointInfo<GroupDiffpointProperties>) diffpoint);
        }

        return result;
    }

    private boolean groupDiffpointWork(ThreadReference thread, DiffpointInfo<GroupDiffpointProperties> diffpointInfo)
            throws IncompatibleThreadStateException, AbsentInformationException, ClassNotLoadedException {
        StackFrame frame = thread.frame(0);

        diffpointInfo.incrementHitCount();
        if (diffpointInfo.isFollowingIterations())
            diffpointInfo.setIterationSelected(diffpointInfo.getHitCount() - 1);
        List<DiffpointInfo<?>> groupDiffpoints = diffpoints.get(GroupDiffpointBreakpointType.class);

        int position = getDiffpointsPositionWithinGroup(diffpointInfo, groupDiffpoints);

        RootNode rootNode = new RootNode(diffpointInfo.getHitCount(), position, diffpointInfo.getId(),
                diffpointInfo.getName());
        for (LocalVariable var : frame.visibleVariables()) {
            Value val = frame.getValue(var);
            createValueTree(var.name(), val, var.type(), rootNode, rootNode);
        }
        diffpointInfo.addRootNode(rootNode);

        List<DiffpointInfo<GroupDiffpointProperties>> allGroupDiffpoints = getGroupDiffpointsByGroupName(
                diffpointInfo.getGroupName());

        project.getMessageBus()
                .syncPublisher(DiffpointListener.TOPIC)
                .groupDiffpointHit(allGroupDiffpoints);

        return diffpointInfo.shouldPause();
    }

    private int getDiffpointsPositionWithinGroup(DiffpointInfo<GroupDiffpointProperties> diffpointInfo,
            List<DiffpointInfo<?>> groupDiffpoints) {
        int position = 0;
        if (diffpointInfo.getRootNodeAt(0) != null && diffpointInfo.getRootNodeAt(0).getPosition() >= 0) {
            position = diffpointInfo.getRootNodeAt(0).getPosition();
        } else {
            for (DiffpointInfo<?> diffpoint : groupDiffpoints) {
                if (!(diffpoint.getProperties() instanceof GroupDiffpointProperties))
                    continue;
                if (diffpoint.getRootNodeAt(0) != null && diffpoint.getRootNodeAt(0).getDiffpointId() != null
                        && diffpoint.getRootNodeAt(0).getPosition() >= 0
                        && !diffpoint.getRootNodeAt(0).getDiffpointId().equals(diffpointInfo.getId())) {
                    position = Math.max((position + 1), diffpoint.getRootNodeAt(0).getPosition());
                }
            }
        }
        return position;
    }

    private boolean lineDiffpointWork(ThreadReference thread, DiffpointInfo<LineDiffpointProperties> diffpointInfo)
            throws IncompatibleThreadStateException, AbsentInformationException, ClassNotLoadedException {
        StackFrame frame = thread.frame(0);

        DiffpointSaveMode mode = diffpointInfo.getMode();
        boolean shouldPause = diffpointInfo.shouldPause();
        diffpointInfo.incrementHitCount();

        if (mode == DiffpointSaveMode.APPEND) {
            Runtime runtime = Runtime.getRuntime();
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            long gatherStart = System.nanoTime();

            RootNode rootNode = new RootNode(diffpointInfo.getHitCount());

            for (LocalVariable var : frame.visibleVariables()) {
                Value val = frame.getValue(var);
                createValueTree(var.name(), val, var.type(), rootNode, rootNode);
            }
            diffpointInfo.addRootNode(rootNode);

            long gatherStop = System.nanoTime();
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("Gathering values took: %.3f ms | Memory overhead: %.3f MB%n",
                    (gatherStop - gatherStart) / 1_000_000.0,
                    (memAfter - memBefore) / (1024.0 * 1024.0));
        } else if (mode == DiffpointSaveMode.CIRCULAR) {
            RootNode rootNode = new RootNode(diffpointInfo.getHitCount());

            for (LocalVariable var : frame.visibleVariables()) {
                Value val = frame.getValue(var);
                createValueTree(var.name(), val, var.type(), rootNode, rootNode);
            }
            diffpointInfo.addRootNode(rootNode);

            if (diffpointInfo.getDataSize() > diffpointInfo.getCircularLimit())
                diffpointInfo.getData().remove(0);
        } else if (mode == DiffpointSaveMode.COMPARE_FIRST) {
            RootNode rootNode = new RootNode(diffpointInfo.getHitCount());

            for (LocalVariable var : frame.visibleVariables()) {
                Value val = frame.getValue(var);
                createValueTree(var.name(), val, var.type(), rootNode, rootNode);
            }

            if (diffpointInfo.getDataSize() == 0)
                diffpointInfo.addRootNode(rootNode);
            else
                diffpointInfo.addToSecondPosition(rootNode);
            shouldPause = true;
        } else if (mode == DiffpointSaveMode.SELECTED_ITERATIONS) {
            ArrayList<Integer> selectedIterations = diffpointInfo.getSelectedIterations();
            if (selectedIterations.contains(diffpointInfo.getHitCount())) {
                RootNode rootNode = new RootNode(diffpointInfo.getHitCount());

                for (LocalVariable var : frame.visibleVariables()) {
                    Value val = frame.getValue(var);
                    createValueTree(var.name(), val, var.type(), rootNode, rootNode);
                }
                diffpointInfo.addRootNode(rootNode);

                project.getMessageBus()
                        .syncPublisher(DiffpointListener.TOPIC)
                        .lineDiffpointHit(diffpointInfo);
                return shouldPause;
            }
            return false;
        }

        project.getMessageBus()
                .syncPublisher(DiffpointListener.TOPIC)
                .lineDiffpointHit(diffpointInfo);

        return shouldPause;
    }

    private void createValueTree(String name, Value val, @Nullable Type type, ObjectNode parentNode,
            RootNode rootNode) {

        String typeName = "";
        if (type != null)
            typeName = type.name().replace("java.lang.", "").replace("java.util.", "");

        if (typeName.contains("."))
            typeName = typeName.substring(typeName.lastIndexOf(".") + 1);

        if (val == null) {
            parentNode.addChild(new NullNode(name));
            return;
        }

        if (val instanceof PrimitiveValue) {
            parentNode.addChild(new TreeNode(name, val.toString(), type));

        } else if (val instanceof StringReference strVal) {
            parentNode.addChild(new StringNode(name, strVal.value(), type));

        } else if (val instanceof ArrayReference arrayRef) {
            ArrayNode arrayNode = new ArrayNode(name, typeName, arrayRef.uniqueID(), type);
            List<Value> values = arrayRef.getValues();
            for (int i = 0; i < values.size(); i++) {
                Value element = values.get(i);
                if (element == null)
                    continue;
                createValueTree(i + "", element, element.type(), arrayNode, rootNode);
            }
            parentNode.addChild(arrayNode);

        } else if (val instanceof ObjectReference objRef) {
            ReferenceType refType = objRef.referenceType();
            long id = objRef.uniqueID();
            if (rootNode.alreadyVisitedObject(id)) {
                // Already visited this object, create a ReferenceNode
                ReferenceNode referenceNode = new ReferenceNode(id, parentNode, typeName, name);
                parentNode.addChild(referenceNode);
                return;
            }
            ObjectNode objectNode = new ObjectNode(name, typeName, objRef.uniqueID(), type);
            String className = refType.name();

            boolean isArrayList = className.equals("java.util.ArrayList");
            boolean isLinkedList = className.equals("java.util.LinkedList");

            boolean isMap = className.equals("java.util.HashMap");
            boolean isEnum = refType instanceof ClassType classType && classType.isEnum();

            if (isArrayList) {
                Field elementData = refType.fieldByName("elementData");
                Field sizeField = refType.fieldByName("size");

                ArrayReference array = (ArrayReference) objRef.getValue(elementData);
                int size = ((IntegerValue) objRef.getValue(sizeField)).value();

                objectNode = new ListNode(name, "ArrayList", objRef.uniqueID(), type);
                List<Value> values = array.getValues(0, size);
                for (int i = 0; i < size; i++) {
                    Value v = values.get(i);
                    createValueTree(String.valueOf(i), v, v == null ? null : v.type(), objectNode, rootNode);
                }
            } else if (isLinkedList) {
                Field firstField = refType.fieldByName("first");
                ObjectReference node = (ObjectReference) objRef.getValue(firstField);
                objectNode = new ListNode(name, "LinkedList", objRef.uniqueID(), type);
                int index = 0;
                while (node != null) {
                    Value item = node.getValue(node.referenceType().fieldByName("item"));
                    createValueTree(String.valueOf(index++), item, item == null ? null : item.type(), objectNode,
                            rootNode);
                    node = (ObjectReference) node.getValue(node.referenceType().fieldByName("next"));
                }
            } else if (isMap) {
                ArrayReference table = (ArrayReference) objRef.getValue(refType.fieldByName("table"));

                objectNode = new MapNode(name, "HashMap", objRef.uniqueID(), type);
                for (Value bucket : table.getValues()) {
                    ObjectReference node = (ObjectReference) bucket;
                    while (node != null) {
                        Value key = node.getValue(node.referenceType().fieldByName("key"));
                        Value bucketVal = node.getValue(node.referenceType().fieldByName("value"));

                        createValueTree(key.toString(), key, key.type(), objectNode, rootNode);
                        createValueTree(bucketVal == null ? "idk" : bucketVal.toString(), bucketVal,
                                bucketVal == null ? null : bucketVal.type(), objectNode, rootNode);

                        node = (ObjectReference) node.getValue(node.referenceType().fieldByName("next"));
                    }
                }
            } else {
                if (isEnum)
                    objectNode = new EnumNode(name, typeName, objRef.uniqueID(), type);
                else
                    rootNode.getVisitedObjects().put(id, objectNode);

                if (refType instanceof ClassType classType) {
                    ClassType superclass = classType.superclass();
                    if (superclass != null && !superclass.name().equals("java.lang.Object")) {
                        objectNode.setSuperclassName(superclass.name());
                    }
                    List<InterfaceType> implementedInterfaces = classType.allInterfaces();
                    for (InterfaceType interfaceType : implementedInterfaces) {
                        objectNode.addImplementedInterface(interfaceType.name());
                    }
                }

                List<Field> instanceFields = new ArrayList<>();
                List<Field> fields = refType.allFields();
                for (Field field : fields) {
                    if (!field.isStatic()) {
                        instanceFields.add(field);
                    }
                }
                Map<Field, Value> fieldValues = objRef.getValues(instanceFields);
                for (Field field : instanceFields) {
                    Value fieldValue = fieldValues.get(field);
                    createValueTree(field.name(), fieldValue, fieldValue == null ? null : fieldValue.type(), objectNode,
                            rootNode);
                }
            }
            parentNode.addChild(objectNode);

        } else {
            System.out.println(name + " = (Unknown value type)");
        }
    }

}
