package settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import data.Matcher;
import utils.Notification;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class SettingsGUI {
    private JTextField textFieldEndpoint;
    private JComboBox comboBoxDiffer;
    private JButton buttonRefresh;
    private JPanel rootPanel;
    private JSlider sliderDepth;

    private CommitClassifyConfig classifyConfig;

    public void createUI(Project project) {
        classifyConfig = CommitClassifyConfig.getInstance(project);
        textFieldEndpoint.setText(classifyConfig.getEndpointURL());
        refreshDiffers();

        comboBoxDiffer.setSelectedItem(classifyConfig.getDiffer());
        sliderDepth.setValue(classifyConfig.getDepth());
        buttonRefresh.addActionListener(e -> refreshDiffers());
    }

    private void refreshDiffers() {
        try {
            List<Matcher> matchers = classifyConfig.loadDiffers(textFieldEndpoint.getText());
            comboBoxDiffer.removeAllItems();
            for (Matcher matcher : matchers) {
                comboBoxDiffer.addItem(matcher);
            }
            comboBoxDiffer.setSelectedIndex(0);
        } catch (Exception e) {
            comboBoxDiffer.removeAllItems();
            Notification.notifyError("Invalid Webservice", "URL does not point to a valid Differ Webservice!");
        }
    }

    public boolean isModified() {
        boolean modified = false;
        modified |= !textFieldEndpoint.getText().equals(classifyConfig.getEndpointURL());
        if (!(comboBoxDiffer.getSelectedItem() == null))
            modified |= !(classifyConfig.getDiffer().equals(comboBoxDiffer.getSelectedItem()));
        modified |= sliderDepth.getValue() != classifyConfig.getDepth();
        return modified;
    }

    public void apply() throws ConfigurationException {
        try {
            URL url = new URL(textFieldEndpoint.getText());
            url.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new ConfigurationException("Invalid URL.");
        }
        classifyConfig.setEndpointURL(textFieldEndpoint.getText());
        if (comboBoxDiffer.getSelectedItem() == null) throw new ConfigurationException("Invalid Differ selection.");
        classifyConfig.setDiffer(((Matcher) comboBoxDiffer.getSelectedItem()));
        classifyConfig.setDepth(sliderDepth.getValue());
    }

    public void reset() {
        textFieldEndpoint.setText(classifyConfig.endpointURL);
        sliderDepth.setValue(classifyConfig.getDepth());
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public JTextField getTextFieldEndpoint() {
        return textFieldEndpoint;
    }

    public JComboBox getComboBoxDiffer() {
        return comboBoxDiffer;
    }
}
