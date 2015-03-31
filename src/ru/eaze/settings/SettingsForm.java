package ru.eaze.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileSystemTree;
import com.intellij.openapi.fileChooser.FileSystemTreeFactory;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeFactoryImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class SettingsForm extends JPanel implements Disposable {

    private static final String LABEL_WEB_DIR = "Web directory name";
    private static final String LABEL_ENABLE_PAGES_CACHE_CHECKSUM = "Verify checksum for pages.xml cache";

    final private FileSystemTree webDir;
    final private JCheckBox enablePagesCacheChecksum;

    private VirtualFile initialWebDir;
    private boolean initialEnablePagesCacheChecksum;

    public SettingsForm(@NotNull Settings settings) {
        setLayout(new GridLayoutManager(2, 1));

        @NotNull final JPanel treePanel = new JPanel(new BorderLayout());
        @NotNull final Border border = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(3, 5, 3, 5));
        treePanel.setBorder(new TitledBorder(border, LABEL_WEB_DIR));
        FileSystemTreeFactory treeFactory = new FileSystemTreeFactoryImpl();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.withTreeRootVisible(true);
        descriptor.setRoots(settings.getProject().getBaseDir());
        webDir = treeFactory.createFileSystemTree(settings.getProject(), descriptor);
        treePanel.add(webDir.getTree(), BorderLayout.CENTER);
        add(treePanel, new GridConstraints(0, 0, 1, 1,GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                null, null, null));

        enablePagesCacheChecksum = new JCheckBox(LABEL_ENABLE_PAGES_CACHE_CHECKSUM);
        add(enablePagesCacheChecksum, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
                null, null, null));

        reset(settings);
    }

    public boolean isModified() {
        return isWebDirModified() || isEnablePagesCacheChecksumModified();
    }

    private boolean isWebDirModified() {
        VirtualFile selection = webDir.getSelectedFile();
        return selection != null && !selection.equals(initialWebDir);
    }

    private boolean isEnablePagesCacheChecksumModified() {
        return enablePagesCacheChecksum.isSelected() != initialEnablePagesCacheChecksum;
    }

    public void reset(Settings settings) {
        resetWebDir(settings);
        resetEnablePagesCacheChecksum(settings);
    }

    private void resetWebDir(Settings settings) {
        String webDirPath = settings.getWebDir();
        VirtualFile webDirFile = settings.getProject().getBaseDir().findFileByRelativePath(webDirPath);
        if (webDirFile != null) {
            webDir.select(webDirFile, null);
            initialWebDir = webDirFile;
        } else {
            webDir.getTree().clearSelection();
        }
    }

    private void resetEnablePagesCacheChecksum(Settings settings) {
        boolean enabled = settings.isPagesCacheChecksumEnabled();
        enablePagesCacheChecksum.setSelected(enabled);
        initialEnablePagesCacheChecksum = enabled;
    }

    public void apply(Settings settings) {
        applyWebDir(settings);
        applyEnablePagesCacheChecksum(settings);
    }

    private void applyWebDir(Settings settings) {
        VirtualFile selectedFile = webDir.getSelectedFile();
        if (selectedFile != null && settings.getProject().getBasePath() != null) {
            String webDirPath = selectedFile.getPath().replace(settings.getProject().getBasePath(), "");
            settings.setWebDir(webDirPath);
            initialWebDir = selectedFile;
        }
    }

    private void applyEnablePagesCacheChecksum(Settings settings) {
        boolean selected = enablePagesCacheChecksum.isSelected();
        settings.setPagesCacheChecksumEnabled(selected);
        initialEnablePagesCacheChecksum = selected;
    }

    @Override
    public void dispose() {
        webDir.dispose();
    }
}
