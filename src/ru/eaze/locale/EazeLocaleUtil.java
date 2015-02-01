package ru.eaze.locale;

import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.xerces.util.XMLChar;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class EazeLocaleUtil {

    private EazeLocaleUtil() { }

    @NonNls
    protected static final String LOCAL_FILE_ROOT_TAG_NAME = "language";
    @NonNls
    protected static final String LOCALE_KEY_DELIMITER = ".";

    private static final Pattern LOCALE_KEY_SPLIT_PATTERN = Pattern.compile(Pattern.quote(LOCALE_KEY_DELIMITER));

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

    public static boolean isValueTag(XmlTag tag) {
        return tag != null
                && tag.isValid()
                && tag.getSubTags().length == 0
                && !tag.getValue().getText().isEmpty();
    }
}
