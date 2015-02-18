package ru.eaze.indexes;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.EazeLocaleDeclarationSearcher;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EazeLocaleUsagesIndex extends FileBasedIndexExtension<String, Collection<Integer>> {

    private static  final Pattern LANG_PATTERN = Pattern.compile("(?:(\\{lang:.+\\}))");

    @NonNls
    public static final ID<String, Collection<Integer>> NAME = ID.create("eazestorm.EazeLocaleUsagesIndex");

    private final KeyDescriptor<String> keyDescriptor = new EnumeratorStringDescriptor();
    private final DataIndexer<String, Collection<Integer>, FileContent> dataIndexer = new EazeLocaleUsagesIndexer();
    private final FileBasedIndex.InputFilter inputFilter = new DefaultFileTypeSpecificInputFilter(PhpFileType.INSTANCE);

    @NotNull
    @Override
    public ID<String, Collection<Integer>> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, Collection<Integer>, FileContent> getIndexer() {
        return dataIndexer;
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return keyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<Collection<Integer>> getValueExternalizer() {
        return new DataExternalizer<Collection<Integer>>() {
            @Override
            public void save(@NotNull DataOutput out, Collection<Integer> entries) throws IOException {
                int size = entries.size();
                DataInputOutputUtil.writeINT(out, size);
                for(Integer entry : entries) {
                    DataInputOutputUtil.writeINT(out, entry);
                }
            }

            @Override
            public Collection<Integer> read(@NotNull DataInput in) throws IOException {
                int length = DataInputOutputUtil.readINT(in);
                Collection<Integer> entries = new TreeSet<Integer>();
                while (length-- > 0) {
                    entries.add(DataInputOutputUtil.readINT(in));
                }
                return entries;
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

    private class EazeLocaleUsagesIndexer implements DataIndexer<String, Collection<Integer>, FileContent> {
        @NotNull
        @Override
        public Map<String, Collection<Integer>> map(@NotNull FileContent inputData) {
            VirtualFile file = inputData.getFile();
            EazeProjectStructure structure = EazeProjectStructure.forProject(inputData.getProject());
            if (structure != null && structure.projectScope().contains(file)) {
                PsiFile psiFile = inputData.getPsiFile();
                Collection<EazeLocaleDeclaration> elements = collectElements(psiFile);
                Map<String, Collection<Integer>> result = new HashMap<String, Collection<Integer>>();
                for (EazeLocaleDeclaration element : elements) {
                    String key = element.getValue();
                    Collection<Integer> offsets = result.get(key);
                    if (offsets == null) {
                        offsets = new TreeSet<Integer>();
                        result.put(key, offsets);
                    }
                    offsets.add(element.getValueTextRange().getStartOffset());
                }
                return result;
            }
            return Collections.emptyMap();
        }

        private Collection<EazeLocaleDeclaration> collectElements(final PsiFile file) {
            final Collection<EazeLocaleDeclaration> elements = new ArrayList<EazeLocaleDeclaration>();
            PsiTreeUtil.processElements(file, new PsiElementProcessor() {
                /**
                 * Processes a PsiElement
                 *
                 * @param element currently processed element.
                 * @return false to stop processing.
                 */
                @Override
                public boolean execute(@NotNull PsiElement element) {
                    if (element.getNode().getElementType() == PhpElementTypes.HTML) {
                        Matcher matcher = LANG_PATTERN.matcher(element.getText());
                        while (matcher.find()) {
                            int offset = element.getTextOffset() + matcher.start(1);
                            PsiElement el = PsiUtilCore.getElementAtOffset(file, offset);
                            EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(el, false);
                            if (declaration != null) {
                                elements.add(declaration);
                            }
                        }
                    } else {
                        EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(element, false);
                        if (declaration != null) {
                            elements.add(declaration);
                        }
                    }
                    return true;
                }
            });
            return elements;
        }
    }
}
