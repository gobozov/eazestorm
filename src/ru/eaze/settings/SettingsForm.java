package ru.eaze.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileSystemTree;
import com.intellij.openapi.fileChooser.FileSystemTreeFactory;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeFactoryImpl;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

public class SettingsForm extends JPanel implements Disposable {

    private boolean isModified;

    final private FileSystemTree fileTree;
    private VirtualFile initialSelectedFile;

    public SettingsForm(@NotNull Settings settings) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        @NotNull final JPanel treePanel = new JPanel(new BorderLayout());
        @NotNull final Border border = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(3, 5, 3, 5));
        treePanel.setBorder(new TitledBorder(border, "EazeStorm Settings"));

        @NotNull final JPanel gridWrapper = new JPanel(new GridLayout(1, 2));
        gridWrapper.add(new JLabel("Web directory name"));
        gridWrapper.add(new JLabel());
        treePanel.add(gridWrapper, BorderLayout.NORTH);

        FileSystemTreeFactory treeFactory = new FileSystemTreeFactoryImpl();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.withTreeRootVisible(true);
        descriptor.setRoots(settings.getProject().getBaseDir());
        fileTree = treeFactory.createFileSystemTree(settings.getProject(), descriptor);
        fileTree.addListener(new FileSystemTree.Listener() {
            @Override
            public void selectionChanged(List<VirtualFile> selection) {
                isModified = !selection.isEmpty() && selection.get(0) != null && !selection.get(0).equals(initialSelectedFile);
            }
        }, fileTree);
        treePanel.add(fileTree.getTree(), BorderLayout.CENTER);
        add(treePanel);

        reset(settings);
    }

    public boolean isModified() {
        return isModified;
    }

    public void reset(Settings settings) {
        String webDirPath = settings.getWebDir();
        VirtualFile webDir = settings.getProject().getBaseDir().findFileByRelativePath(webDirPath);
        if (webDir != null) {
            fileTree.select(webDir, null);
            initialSelectedFile = webDir;
        } else {
            fileTree.getTree().clearSelection();
        }
        isModified = false;
    }

    public void apply(Settings settings) {
        VirtualFile selectedFile = fileTree.getSelectedFile();
        if (selectedFile != null) {
            String webDirPath = selectedFile.getPath().replace(settings.getProject().getBasePath(), "");
            settings.setWebDir(webDirPath);
            initialSelectedFile = selectedFile;
        }
        isModified = false;
    }

    @Override
    public void dispose() {
        fileTree.dispose();
    }
}
