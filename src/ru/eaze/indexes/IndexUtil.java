package ru.eaze.indexes;

import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;

public class IndexUtil {

    private IndexUtil() { }

    public static void reindex() {
        ID<?,?>[] indexes = {
                EazeActionsIndex.NAME,
                EazePathIndex.NAME,
                EazeLocaleKeyIndex.NAME,
                EazeLocaleUsagesIndex.NAME,
                EazeLocaleKeyPrefixIndex.NAME
        };
        for(ID<?,?> id: indexes) {
            FileBasedIndex.getInstance().requestRebuild(id);
            FileBasedIndex.getInstance().scheduleRebuild(id, new Throwable());
        }
    }
}
