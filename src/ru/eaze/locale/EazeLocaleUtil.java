package ru.eaze.locale;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.xerces.util.XMLChar;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.indexes.EazeLocaleKeyIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Pattern;

public class EazeLocaleUtil {

    private EazeLocaleUtil() { }

    @NonNls
    private static final String LOCAL_FILE_ROOT_TAG_NAME = "language";
    @NonNls
    public static final String LOCALE_KEY_DELIMITER = ".";

    private static final Pattern LOCALE_KEY_SPLIT_PATTERN = Pattern.compile(Pattern.quote(LOCALE_KEY_DELIMITER));

    public static boolean isValidKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String[] keyParts = LOCALE_KEY_SPLIT_PATTERN.split(key);
        if (keyParts.length == 0) { //dots only
            return false;
        }
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
        String[] tags = LOCALE_KEY_SPLIT_PATTERN.split(key);
        if (tags.length == 0 ||(tags.length == 1 && !key.endsWith(LOCALE_KEY_DELIMITER))) {
            return false;
        }
        Collection<VirtualFile> files = getLocaleFiles(project);
        for (VirtualFile file : files) {
            XmlTag tag = findTagForKey(project, file, tags[0]);
            if (tag != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean canCreateKey(Project project, Collection<VirtualFile> files, String key) {
        if (project == null) {
            return false;
        }
        if (files == null || files.isEmpty()) {
            return false;
        }
        if (!isValidKey(key)) {
            return false;
        }
        for (VirtualFile file : files) {
            if (file == null || !file.isValid()) {
                continue;
            }
            //check key
            XmlTag tag = findTagForKey(project, file, key);
            if (tag != null) {
                if (tag.getSubTags().length == 0 && tag.getValue().getTrimmedText().isEmpty())
                    continue;
                else
                    return false;
            }
            //check parents
            {
                int end = key.lastIndexOf(LOCALE_KEY_DELIMITER);
                String subKey = end < 0 ? "" : key.substring(0, end);
                while (subKey.length() > 0) {
                    tag = findTagForKey(project, file, subKey);
                    if (tag != null) {
                        if (isValueTag(tag))
                            return false;
                        else
                            break;  //acceptable parent
                    }
                    end = subKey.lastIndexOf(LOCALE_KEY_DELIMITER);
                    subKey = end < 0 ? "" : subKey.substring(0, end);
                }
            }
        }
        return true;
    }

    public static boolean canCreateKey(Project project, String key) {
        return canCreateKey(project, getLocaleFiles(project), key);
    }

    @NotNull
    public static String findKeyInString(@NotNull String str) {
        StringBuilder key = new StringBuilder(str.length());
        String[] keyParts = LOCALE_KEY_SPLIT_PATTERN.split(str);
        for (String keyPart : keyParts) {
            if (XMLChar.isValidName(keyPart)) {
                key.append(keyPart);
                if (key.length() < str.length()) {
                    key.append(LOCALE_KEY_DELIMITER);
                }
            } else {
                break;
            }
        }
        return key.toString();
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

    @NotNull
    public static Collection<XmlTag> findTagsForKey(Project project, String key) {
        Collection<XmlTag> tags = new ArrayList<XmlTag>();
        for (VirtualFile file : getLocaleFiles(project)) {
            XmlTag tag = findTagForKey(project, file, key);
            if (tag != null) {
                tags.add(tag);
            }
        }
        return tags;
    }

    public static boolean isValueTag(XmlTag tag) {
        return tag != null
                && tag.isValid()
                && tag.getSubTags().length == 0
                && !tag.getValue().getTrimmedText().isEmpty();
    }

    @NotNull
    public static String extractTagValue(XmlTag tag) {
        if (!isValueTag(tag)) {
            return "";
        }
        return tag.getValue().getTrimmedText();
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
        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(EazeLocaleKeyIndex.NAME, key, structure.projectScope());
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
            XmlTag root = xmlFile.getRootTag();
            if (root != null && root.isValid()) {
                XmlAttribute languageName = root.getAttribute("name");
                if (languageName != null && locale.getLanguage().equals(languageName.getValue())) {
                    text = extractTagValue(tag) + " [" + file.getName() + "]";
                    return text;
                }
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
        return structure != null && structure.projectScope().contains(file);
    }

    @NotNull
    public static Collection<VirtualFile> getLocaleFiles(Project project) {
        EazeProjectStructure structure = EazeProjectStructure.forProject(project);
        if (structure == null) {
            return Collections.emptyList();
        }
        VirtualFile dir = structure.localeDirectory();
        if (dir == null || !dir.isValid() || !dir.isDirectory()) {
            return  Collections.emptyList();
        }
        Collection<VirtualFile> files = new ArrayList<VirtualFile>();
        VirtualFile[] dirFiles = dir.getChildren();
        for (VirtualFile file : dirFiles) {
            if (file.isValid() && file.getFileType() == XmlFileType.INSTANCE) {
                XmlFile xmlFile = (XmlFile)PsiManager.getInstance(project).findFile(file);
                XmlTag root = xmlFile == null ? null : xmlFile.getRootTag();
                if (root!= null && root.isValid() && root.getName().equals(LOCAL_FILE_ROOT_TAG_NAME)) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public static boolean isLocaleFile(VirtualFile file, Project project) {
        if (file == null || !file.isValid() || file.getFileType() != XmlFileType.INSTANCE) {
            return false;
        }
        EazeProjectStructure structure = EazeProjectStructure.forProject(project);
        if (structure == null || !structure.projectScope().contains(file)) {
            return false;
        }
        VirtualFile dir = structure.localeDirectory();
        if (dir == null || !dir.isValid() || !dir.isDirectory()) {
            return false;
        }
        if (dir.equals(file.getParent())) {
            XmlFile xmlFile = (XmlFile)PsiManager.getInstance(project).findFile(file);
            XmlTag root = xmlFile == null ? null : xmlFile.getRootTag();
            return root!= null && root.isValid() && root.getName().equals(LOCAL_FILE_ROOT_TAG_NAME);
        }
        return false;
    }
}
