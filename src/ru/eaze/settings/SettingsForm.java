package ru.eaze.settings;

import com.intellij.ui.IdeBorderFactory;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: GGobozov
 * Date: 29.11.12
 * Time: 15:21
 * To change this template use File | Settings | File Templates.
 */
public class SettingsForm extends JPanel {
    @NotNull
    private JTextField webDirTextField;
    @NotNull
    private Settings settings;
    private boolean isModified;

    public SettingsForm(Settings settings) {
        this.settings = settings;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        @NotNull final JPanel cachePanel = new JPanel(new BorderLayout());
        @NotNull final Border border = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(3, 5, 3, 5));
        cachePanel.setBorder(new TitledBorder(border, "EazeStorm Settings"));

        @NotNull final JPanel gridWrapper = new JPanel(new GridLayout(1, 2));
        gridWrapper.add(new JLabel("Web directory name"));
        gridWrapper.add(new JLabel());
        cachePanel.add(gridWrapper, BorderLayout.NORTH);

        webDirTextField = new JTextField(settings.getStringValue(Settings.KEY_WEB_DIR, "web"));

        webDirTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                isModified = true;
            }

            public void removeUpdate(DocumentEvent e) {
                isModified = true;
            }

            public void insertUpdate(DocumentEvent e) {
                isModified = true;
            }
        });


        @NotNull JPanel buttonWrapper = new JPanel(new GridLayout(1, 3));
        buttonWrapper.setBorder(IdeBorderFactory.createEmptyBorder(3));
        buttonWrapper.add(webDirTextField);
        buttonWrapper.add(new Spacer());
        buttonWrapper.add(new Spacer());
        cachePanel.add(buttonWrapper, BorderLayout.SOUTH);

        cachePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, cachePanel.getPreferredSize().height));
        add(cachePanel);

    }

    @NotNull
    public JTextField getWebDirTextField() {
        return webDirTextField;
    }

    public boolean isModified() {
        return isModified;
    }

    @NotNull
    public Settings getSettings() {
        return settings;
    }

    public void setSettings(@NotNull Settings settings) {
        this.settings = settings;
    }
}
