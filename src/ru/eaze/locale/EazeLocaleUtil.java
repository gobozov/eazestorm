package ru.eaze.locale;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.xerces.util.XMLChar;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeProjectStructure;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

public class EazeLocaleUtil {

    private EazeLocaleUtil() { }

    @NonNls
    public static final String LOCAL_FILE_ROOT_TAG_NAME = "language";
    @NonNls
    public static final String LOCALE_KEY_DELIMITER = ".";

    protected static final Pattern LOCALE_KEY_SPLIT_PATTERN = Pattern.compile(Pattern.quote(LOCALE_KEY_DELIMITER));

    public static boolean isValidKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String[] keyParts = LOCALE_KEY_SPLIT_PATTERN.split(key);
        for (String keyPart : keyParts) {
            if (!XMLChar.isValidName(keyPart)) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepIsValidKey(String key, Project project) {
        if (!isValidKey(key)) {
            return false;
        }
        EazeProjectStructure structure = EazeProjectStructure.forProject(project);
        if (structure == null) {
            return false;
        }
        String[] tags = LOCALE_KEY_SPLIT_PATTERN.split(key);
        Collection<VirtualFile> files = structure.localeFiles();
        for (VirtualFile file : files) {
            XmlTag tag = findTagForKey(project, file, tags[0]);
            if (tag != null) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static XmlTag findTagForKey(XmlFile file, String key) {
        if (file == null || !file.isValid()) {
            return null;
        }
        if (!EazeLocaleUtil.isValidKey(key)) {
            return null;
        }
        XmlTag root = file.getRootTag();
        if (root != null && root.isValid() && root.getName().equals(EazeLocaleUtil.LOCAL_FILE_ROOT_TAG_NAME)) {
            String[] tagNames = EazeLocaleUtil.LOCALE_KEY_SPLIT_PATTERN.split(key);
            XmlTag tag = root;
            for (String tagName : tagNames) {
                tag = tag.findFirstSubTag(tagName);
                if (tag == null || !tag.isValid()) {
                    return null;
                }
            }
            return tag;
        }
        return null;
    }

    @Nullable
    public static XmlTag findTagForKey(Project project, VirtualFile file, String key) {
        if (project == null) {
            return null;
        }
        if (file == null || !file.isValid()) {
            return null;
        }
        if (!EazeLocaleUtil.isValidKey(key)) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile instanceof XmlFile && psiFile.isValid()) {
            return findTagForKey((XmlFile)psiFile, key);
        }
        return null;
    }

    public static boolean isValueTag(XmlTag tag) {
        return tag != null
                && tag.isValid()
                && tag.getSubTags().length == 0
                && !tag.getValue().getText().isEmpty();
    }

    @NotNull
    public static String extractTagValue(XmlTag tag) {
        if (!isValueTag(tag)) {
            return "";
        }
        String value = tag.getValue().getTrimmedText();
        return value;
    }

    public static String[] getKeyParts(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return LOCALE_KEY_SPLIT_PATTERN.split(key);
    }

    @NotNull
    public static String createTextForAnnotation(String key, Project project) {
        String text = "";
        EazeProjectStructure structure = EazeProjectStructure.forProject(project);
        if (structure == null) {
            return text;
        }
        Locale locale = Locale.getDefault();
        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(EazeLocaleKeyIndex.NAME, key, EazeProjectStructure.forProject(project).projectScope());
        for (VirtualFile file : files) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null || !psiFile.isValid() || !(psiFile instanceof XmlFile)) {
                continue;
            }
            XmlFile xmlFile = (XmlFile) psiFile;
            XmlTag tag = findTagForKey(xmlFile, key);
            if (!isValueTag(tag)) {
                continue;
            }
            if (text.isEmpty()) {
                text = extractTagValue(tag) + " [" + file.getName() + "]";
            }
            XmlAttribute languageName = xmlFile.getRootTag().getAttribute("name");
            if (languageName != null && locale.getLanguage().equals(languageName.getValue())) {
                text = extractTagValue(tag) + " [" + file.getName() + "]";
                return text;
            }
        }
        return text;
    }

    public static boolean inScope(PsiElement element) {
        if (element == null || !element.isValid()) {
            return false;
        }
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null || !psiFile.isValid()) {
            return false;
        }
        VirtualFile file = psiFile.getVirtualFile();
        if (file == null || !file.isValid()) {
            return false;
        }
        EazeProjectStructure structure = EazeProjectStructure.forProject(element.getProject());
        if (structure == null) {
            return false;
        }
        return structure.projectScope().contains(file);
    }
}
