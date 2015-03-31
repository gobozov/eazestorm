package ru.eaze.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import ru.eaze.locale.EazeLocaleUtil;

import java.util.*;

public class EazeLocaleKeyPrefixIndex extends ScalarIndexExtension<String> {

    @NonNls
    public static final ID<String, Void> NAME = ID.create("eazestorm.EazeLocaleKeyPrefixIndex");

    private final KeyDescriptor<String> keyDescriptor = new EnumeratorStringDescriptor();
    private final DataIndexer<String, Void, FileContent> dataIndexer = new EazeLocaleKeyPrefixIndexer();
    private final FileBasedIndex.InputFilter inputFilter = new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE);

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return dataIndexer;
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return keyDescriptor;
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

    private class EazeLocaleKeyPrefixIndexer implements DataIndexer<String, Void, FileContent> {
        @NotNull
        @Override
        public Map<String, Void> map(@NotNull FileContent inputData) {
            if(EazeLocaleUtil.isLocaleFile(inputData.getFile(), inputData.getProject())) {
                XmlTag root = ((XmlFile)inputData.getPsiFile()).getRootTag();
                if (root != null && root.isValid()) {
                    Map<String, Void> result = new HashMap<String, Void>();
                    for (String key : extractPrefixes(root, "")) {
                        result.put(key, null);
                    }
                    return  result;
                }
            }
            return Collections.emptyMap();
        }

        @NotNull
        private Collection<String> extractPrefixes(XmlTag root, @NotNull String prefix) {
            Collection<String> keys = new HashSet<String>();
            for (XmlTag tag : root.getSubTags()) {
                if (tag.isValid() && !EazeLocaleUtil.isValueTag(tag)) {
                    String key = prefix.isEmpty() ? tag.getName() : prefix + EazeLocaleUtil.LOCALE_KEY_DELIMITER + tag.getName();
                    keys.add(key);
                    keys.addAll(extractPrefixes(tag, key));
                }
            }
            return keys;
        }
    }
}
