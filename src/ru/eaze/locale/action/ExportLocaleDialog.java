package ru.eaze.locale.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.locale.EazeLocaleUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExportLocaleDialog extends JDialog {

    private static final String TITLE = "Export Localization";
    private static final String EMPTY_TEXT = "Text is empty";
    private static final String EMPTY_KEY = "Key is empty";
    private static final String NO_FILES = "Localization files not found";
    private static final String NO_FILES_SELECTED = "Select files";
    private static final String INVALID_KEY = "Invalid key";
    private static final String NOT_ACCEPTABLE_KEY = "Key cannot be created";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField localeKey;
    private JPanel filesPanel;
    private JLabel errorMessage;
    private JTextField localeText;

    private Project project;
    private Callback callback;
    private ListTableModel<FileSelectionModel> localeFiles;

    public ExportLocaleDialog(@NotNull Project project, String initialText, String keyPrefix, Callback callback) {
        this.project = project;
        this.callback = callback;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        localeKey.setText(keyPrefix);
        localeKey.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateInput();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateInput();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateInput();
            }
        });

        localeText.setText(initialText);
        localeText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateInput();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateInput();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateInput();
            }
        });

        localeFiles = new ListTableModel<FileSelectionModel>(new IconColumn(), new FilePathColumn(), new BooleanColumn());
        listLocaleFiles();
        TableView<FileSelectionModel> tableView = new TableView<FileSelectionModel>();
        tableView.setModelAndUpdateColumns(localeFiles);
        filesPanel.add(ToolbarDecorator.createDecorator(tableView).disableAddAction().disableDownAction().disableRemoveAction().disableUpDownActions().createPanel(),
                new GridConstraints(0, 0, 1, 1,GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                        GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                        null, null, null));

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonOK.setEnabled(false); //empty key at minimum

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
            public void windowOpened(WindowEvent e) {
                localeKey.requestFocus();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setTitle(TITLE);
    }

    private void listLocaleFiles() {
        Collection<VirtualFile> files = EazeLocaleUtil.getLocaleFiles(project);
        for (VirtualFile file : files) {
            localeFiles.addRow(new FileSelectionModel(file));
        }
        if (files.isEmpty()) {
            errorMessage.setText(NO_FILES);
            buttonOK.setEnabled(false);
        }
    }

    private void validateInput() {
        if (localeFiles.getItems().isEmpty()) {
            errorMessage.setText(NO_FILES);
            buttonOK.setEnabled(false);
            return;
        }
        if (StringUtils.isBlank(localeKey.getText())) {
            errorMessage.setText(EMPTY_KEY);
            buttonOK.setEnabled(false);
            return;
        }
        if (StringUtils.isBlank(localeText.getText())) {
            errorMessage.setText(EMPTY_TEXT);
            buttonOK.setEnabled(false);
            return;
        }
        if (selectedFiles().isEmpty()) {
            errorMessage.setText(NO_FILES_SELECTED);
            buttonOK.setEnabled(false);
            return;
        }
        if (!EazeLocaleUtil.isValidKey(localeKey.getText())) {
            errorMessage.setText(INVALID_KEY);
            buttonOK.setEnabled(false);
            return;
        }
        if (!EazeLocaleUtil.canCreateKey(project, selectedFiles(), localeKey.getText())) {
            errorMessage.setText(NOT_ACCEPTABLE_KEY);
            buttonOK.setEnabled(false);
            return;
        }
        errorMessage.setText("");
        buttonOK.setEnabled(true);
    }

    private List<VirtualFile> selectedFiles() {
        List<VirtualFile> selected = new ArrayList<VirtualFile>();
        for (FileSelectionModel model : localeFiles.getItems()) {
            if (model.isSelected()) {
                selected.add(model.getFile());
            }
        }
        return selected;
    }

    private void onOK() {
        if (callback != null) {
            callback.onOK(selectedFiles(), localeKey.getText(), localeText.getText());
        }
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static interface Callback {
        void onOK(Collection<VirtualFile> files, String key, String text);
    }

    private class FileSelectionModel {
        private final VirtualFile file;
        private boolean selected = true;

        public FileSelectionModel(VirtualFile file) {
            this.file = file;
        }

        public VirtualFile getFile() {
            return file;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    private class IconColumn extends ColumnInfo<FileSelectionModel, Icon> {

        public IconColumn() {
            super("");
        }

        @Nullable
        @Override
        public Icon valueOf(FileSelectionModel model) {
            PsiFile file = PsiManager.getInstance(project).findFile(model.getFile());
            return file == null ? AllIcons.General.Error : file.getIcon(0);
        }

        @Override
        public Class getColumnClass() {
            return ImageIcon.class;
        }

        @Override
        public int getWidth(JTable table) {
            return 32;
        }
    }

    private class FilePathColumn extends ColumnInfo<FileSelectionModel, String> {

        public FilePathColumn() {
            super("Localization File");
        }

        @Nullable
        @Override
        public String valueOf(FileSelectionModel model) {
            String path = model.getFile().getPath();
            path = path.substring(project.getBasePath().length());
            return path;
        }
    }

    private class BooleanColumn extends ColumnInfo<FileSelectionModel, Boolean> {

        public BooleanColumn() {
            super("");
        }

        @Nullable
        @Override
        public Boolean valueOf(FileSelectionModel model) {
            return model.isSelected();
        }

        public boolean isCellEditable(FileSelectionModel model)
        {
            return true;
        }

        public void setValue(FileSelectionModel model, Boolean value){
            model.setSelected(value);
            validateInput();
        }

        public Class getColumnClass()
        {
            return Boolean.class;
        }

        public int getWidth(JTable table) {
            return 50;
        }
    }
}
