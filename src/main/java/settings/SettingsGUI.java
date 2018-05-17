package settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import data.Matcher;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class SettingsGUI {
    private JTextField textFieldEndpoint;
    private JComboBox comboBoxDiffer;
    private JButton buttonRefresh;
    private JPanel rootPanel;

    private CommitClassifyConfig classifyConfig;

    public void createUI(Project project) {
        classifyConfig = CommitClassifyConfig.getInstance(project);
        textFieldEndpoint.setText(classifyConfig.endpointURL);
        refreshDiffers();
        //comboBoxDiffer.addItem("Refresh a valid URL to get matchers");
        comboBoxDiffer.setSelectedItem(classifyConfig.getDiffer());
        buttonRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshDiffers();
            }
        });
    }

    private void refreshDiffers() {
        List<Matcher> matchers = classifyConfig.loadDiffers(textFieldEndpoint.getText());
        comboBoxDiffer.removeAllItems();
        for (Matcher matcher : matchers) {
            comboBoxDiffer.addItem(matcher);
        }
        comboBoxDiffer.setSelectedIndex(0);
    }

    public boolean isModified() {
        boolean modified = false;
        modified |= !textFieldEndpoint.getText().equals(classifyConfig.getEndpointURL());
        if (!(comboBoxDiffer.getSelectedItem() == null))
            modified |= !(classifyConfig.getDiffer().equals(comboBoxDiffer.getSelectedItem()));
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
    }

    public void reset() {
        textFieldEndpoint.setText(classifyConfig.endpointURL);
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
