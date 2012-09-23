package ru.eaze.ui;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: gb
 * Date: 23.09.12
 * Time: 21:23
 * To change this template use File | Settings | File Templates.
 */
public class EazeSettingsPanel  implements Configurable {
    private Panel panel;

    private static class Panel extends JPanel {
        @NotNull
        private final JLabel cacheSizeValueLabel;

        public Panel() {
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

            @NotNull final JPanel cachePanel = new JPanel(new BorderLayout());
            @NotNull final Border border = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(3, 5, 3, 5));
            cachePanel.setBorder(new TitledBorder(border, "EazeStorm Settings"));

            @NotNull final JPanel gridWrapper = new JPanel(new GridLayout(1, 2));
            gridWrapper.add(new JLabel("label"));
            gridWrapper.add(cacheSizeValueLabel = new JLabel());
            cachePanel.add(gridWrapper, BorderLayout.NORTH);

            @NotNull final JButton clearCacheButton = new JButton("button");
            clearCacheButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //ServiceManager.getService(EntryCache.class).clear();
                    //updateCacheSize();
                }
            });

            @NotNull JPanel buttonWrapper = new JPanel(new GridLayout(1, 3));
            buttonWrapper.setBorder(IdeBorderFactory.createEmptyBorder(3));
            buttonWrapper.add(clearCacheButton);
            buttonWrapper.add(new Spacer());
            buttonWrapper.add(new Spacer());
            cachePanel.add(buttonWrapper, BorderLayout.SOUTH);

            cachePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, cachePanel.getPreferredSize().height));
            add(cachePanel);

            updateCacheSize();
        }

        public void updateCacheSize() {
            //final long cacheSizeInBytes = ServiceManager.getService(EntryCache.class).getCacheSizeInBytes();
            //cacheSizeValueLabel.setText(FileUtils.byteCountToDisplaySize(cacheSizeInBytes));
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "EazeStorm";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        if (panel == null) {
            panel = new Panel();
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }
}
