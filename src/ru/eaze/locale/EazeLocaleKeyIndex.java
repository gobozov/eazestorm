package ru.eaze.locale;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.FileBasedIndex.InputFilter;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.locale.EazeLocaleUtil;

import java.util.*;

public class EazeLocaleKeyIndex extends ScalarIndexExtension<String> {

    @NonNls
    public static final ID<String, Void> NAME = ID.create("eazestorm.EazeLocaleKeyIndex");

    private final KeyDescriptor<String> keyDescriptor = new EnumeratorStringDescriptor();
    private final DataIndexer<String, Void, FileContent> dataIndexer = new EazeLocaleKeyIndexer();
    private final InputFilter inputFilter = new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE);

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
    public InputFilter getInputFilter() {
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

    private class EazeLocaleKeyIndexer implements DataIndexer<String, Void, FileContent> {
        @NotNull
        @Override
        public Map<String, Void> map(@NotNull FileContent inputData) {
            EazeProjectStructure structure = EazeProjectStructure.forProject(inputData.getProject());
            if(structure != null && structure.isLocaleFile(inputData.getFile())) {
                XmlTag root = ((XmlFile)inputData.getPsiFile()).getRootTag();
                if (root != null && root.isValid() && root.getName().equals(EazeLocaleUtil.LOCAL_FILE_ROOT_TAG_NAME)) {
                    Map<String, Void> result = new HashMap<String, Void>();
                    for (XmlTag subTag : root.getSubTags()) {
                        if (subTag.isValid()) {
                            for (String key : extractKeys(subTag, "")) {
                                result.put(key, null);
                            }
                        }
                    }
                    return  result;
                }
            }
            return Collections.emptyMap();
        }

        @NotNull
        private Collection<String> extractKeys(XmlTag tag, @NotNull String prefix) {
            Collection<String> keys = new HashSet<String>();
            String key = prefix.isEmpty() ? tag.getName() : prefix + EazeLocaleUtil.LOCALE_KEY_DELIMITER + tag.getName();
            if (tag.getSubTags().length > 0) {
                for (XmlTag subTag : tag.getSubTags()) {
                    if (subTag.isValid()) {
                        keys.addAll(extractKeys(subTag, key));
                    }
                }
            } else if (EazeLocaleUtil.isValueTag(tag)) {
                keys.add(key);
            }
            return keys;
        }
    }

    @NotNull
    public static Collection<XmlTag> findKeyDeclarations(final Project project, final String key) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Collection<XmlTag>>() {
            @Override
            public Collection<XmlTag> compute() {
                final Collection<VirtualFile> files;
                try {
                    files = FileBasedIndex.getInstance().getContainingFiles(NAME, key, GlobalSearchScope.allScope(project));
                } catch (IndexNotReadyException ex) {
                    return Collections.emptyList();
                }
                if (files.isEmpty()) {
                    return Collections.emptyList();
                }
                Collection<XmlTag> result = new ArrayList<XmlTag>();
                for (VirtualFile file : files) {
                    XmlTag tag = EazeLocaleUtil.findTagForKey(project, file, key);
                    if (EazeLocaleUtil.isValueTag(tag)) {
                        result.add(tag);
                    }
                }
                return result;
            }
        });
    }

}
