package sk.tuke.diffpoints.service;

import com.intellij.util.messages.Topic;
import sk.tuke.diffpoints.breakpoints.properties.DiffpointInfo;
import sk.tuke.diffpoints.breakpoints.properties.GroupDiffpointProperties;
import sk.tuke.diffpoints.breakpoints.properties.LineDiffpointProperties;

import java.util.List;

public interface DiffpointListener {

    Topic<DiffpointListener> TOPIC = Topic.create("Diffpoint events", DiffpointListener.class);

    void lineDiffpointHit(DiffpointInfo<LineDiffpointProperties> diffpointInfo);
    void groupDiffpointHit(List<DiffpointInfo<GroupDiffpointProperties>> diffpointInfo);
}

