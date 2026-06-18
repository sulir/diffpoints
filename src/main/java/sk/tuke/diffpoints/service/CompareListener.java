package sk.tuke.diffpoints.service;

import com.intellij.util.messages.Topic;
import sk.tuke.diffpoints.objectSaving.TreeNode;

public interface CompareListener {

    Topic<CompareListener> TOPIC = Topic.create("Compare events", CompareListener.class);

    void nodeAdded(TreeNode node);
    void nodeRemoved(TreeNode node);
    void removeAll();

}
