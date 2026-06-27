package sk.tuke.diffpoints.settings;

import com.intellij.util.messages.Topic;

public interface DiffpointsSettingsListener {

    Topic<DiffpointsSettingsListener> TOPIC = Topic.create(
            "Diffpoints settings changed",
            DiffpointsSettingsListener.class);

    void settingsChanged();
}
