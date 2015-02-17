package ru.eaze.domain;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class EazeActionsIndex extends FileBasedIndexExtension<String, Integer> {

    @NonNls
    public static final ID<String, Integer> NAME = ID.create("eazestorm.EazeActionsIndex");

    private final KeyDescriptor<String> keyDescriptor = new EnumeratorStringDescriptor();
    private final DataIndexer<String, Integer, FileContent> dataIndexer = new EazeActionsIndexer();
    private final FileBasedIndex.InputFilter inputFilter = new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE);

    @NotNull
    @Override
    public ID<String, Integer> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, Integer, FileContent> getIndexer() {
        return dataIndexer;
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return keyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<Integer> getValueExternalizer() {
        return new DataExternalizer<Integer>() {
            @Override
            public void save(@NotNull DataOutput out, Integer value) throws IOException {
                DataInputOutputUtil.writeINT(out, value);
            }

            @Override
            public Integer read(@NotNull DataInput in) throws IOException {
                return DataInputOutputUtil.readINT(in);
            }
        };
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return inputFilter;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    private class EazeActionsIndexer implements DataIndexer<String, Integer, FileContent> {
        @NotNull
        @Override
        public Map<String, Integer> map(@NotNull FileContent inputData) {
            Map<String, Integer> result = new HashMap<String, Integer>();
            EazeProjectStructure structure = EazeProjectStructure.forProject(inputData.getProject());
            if (structure != null && structure.isActionsConfigFile(inputData.getFile())) {
                XmlFile file = (XmlFile) inputData.getPsiFile();
                String packageName = inputData.getFile().getParent() != null ? inputData.getFile().getParent().getName() : "";
                XmlTag root = file.getRootTag();
                if (root != null) {
                    Collection<XmlTag> actions = collectActionTags(root);
                    for (XmlTag action : actions) {
                        XmlAttribute name = action.getAttribute("name");
                        if (name != null) {
                            String actionName = name.getValue();
                            if (actionName != null && !actionName.isEmpty()) {
                                result.put(packageName + "." + actionName, action.getTextOffset());
                            }
                        }
                    }
                }
            }
            return result;
        }

        private Collection<XmlTag> collectActionTags(XmlTag root) {
            Collection<XmlTag> actions = new ArrayList<XmlTag>();
            for (XmlTag tag : root.getSubTags()) {
                if (tag.getName().equals("action")) {
                    actions.add(tag);
                }
                actions.addAll(collectActionTags(tag));
            }
            return actions;
        }
    }
}
