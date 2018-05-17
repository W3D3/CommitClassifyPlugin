package settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ClassifyCommitConfigurable implements SearchableConfigurable {
    SettingsGUI gui;
    Project project;
    CommitClassifyConfig config;

    public ClassifyCommitConfigurable(@NotNull Project project) {
        this.project = project;
        config = CommitClassifyConfig.getInstance(project);
    }

    @NotNull
    @Override
    public String getId() {
        return "at.aau.commitclassifier";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Commit Classification Plugin";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        gui = new SettingsGUI();
        gui.createUI(project);
        return gui.getRootPanel();
    }

    @Override
    public boolean isModified() {
        return gui.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        gui.apply();
    }

    @Override
    public void reset() {
        gui.reset();
    }


}
