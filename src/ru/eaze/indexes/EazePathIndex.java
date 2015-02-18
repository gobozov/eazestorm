package ru.eaze.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.*;
import com.intellij.util.io.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EazePathIndex extends FileBasedIndexExtension<String, List<String>> {

    @NonNls
    public static final ID<String, List<String>> NAME = ID.create("eazestorm.EazePathIndex");

    private final KeyDescriptor<String> keyDescriptor = new EnumeratorStringDescriptor();
    private final DataIndexer<String, List<String>, FileContent> dataIndexer = new EazePathIndexer();
    private final FileBasedIndex.InputFilter inputFilter = new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE);

    @NotNull
    @Override
    public ID<String, List<String>> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, List<String>, FileContent> getIndexer() {
        return dataIndexer;
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return keyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<List<String>> getValueExternalizer() {
        return new DataExternalizer<List<String>>() {
            @Override
            public void save(@NotNull DataOutput out, List<String> values) throws IOException {
                DataInputOutputUtil.writeINT(out, values.size());
                for (String value : values) {
                    IOUtil.writeUTF(out, value);
                }
            }

            @Override
            public List<String> read(@NotNull DataInput in) throws IOException {
                int size = DataInputOutputUtil.readINT(in);
                List<String> values = new ArrayList<String>(size);
                while (size-- > 0) {
                    values.add(IOUtil.readUTF(in));
                }
                return values;
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

    private class EazePathIndexer implements DataIndexer<String, List<String>, FileContent> {
        @NotNull
        @Override
        public Map<String, List<String>> map(@NotNull FileContent inputData) {
            Map<String, List<String>> result = new HashMap<String, List<String>>();
            EazeProjectStructure structure = EazeProjectStructure.forProject(inputData.getProject());
            if (structure != null && structure.isSitesConfigFile(inputData.getFile())) {
                XmlFile file = (XmlFile) inputData.getPsiFile();
                List<XmlTag> pathTags = collectPathTags(file);
                for (XmlTag pathTag : pathTags) {
                    String name = pathTag.getAttributeValue("name");
                    String value = pathTag.getAttributeValue("value");
                    if (name != null && value != null) {
                        List<String> values = result.get(name);
                        if (values == null) {
                            values = new ArrayList<String>();
                            result.put(name, values);
                        }
                        values.add(value);
                    }
                }
            }
            return result;
        }

        private List<XmlTag> collectPathTags(XmlFile file) {
            List<XmlTag> pathTags = new ArrayList<XmlTag>();
            XmlTag root = file.getRootTag();
            if (root != null && root.isValid() && root.getName().equals("sites")) {
                for (XmlTag siteTag : root.getSubTags()) {
                    if (siteTag.getName().equals("site")) {
                        XmlTag settingsTag = siteTag.findFirstSubTag("settings");
                        if (settingsTag != null) {
                            pathTags.addAll(collectPathTags(settingsTag));
                        }

                        XmlTag hostsTag = siteTag.findFirstSubTag("hosts");
                        if (hostsTag != null) {
                            for (XmlTag hostTag : hostsTag.getSubTags()) {
                                if (hostTag.getName().equals("host")) {
                                    XmlTag hostSettingsTag = hostTag.findFirstSubTag("settings");
                                    if (hostSettingsTag != null) {
                                        pathTags.addAll(collectPathTags(hostSettingsTag));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return pathTags;
        }

        private List<XmlTag> collectPathTags(XmlTag settings) {
            List<XmlTag> pathTags = new ArrayList<XmlTag>();
            XmlTag pathsTag = settings.findFirstSubTag("paths");
            if (pathsTag != null) {
                for (XmlTag pathTag : pathsTag.getSubTags()) {
                    if (pathTag.getName().equals("path")) {
                        pathTags.add(pathTag);
                    }
                }
            }
            return pathTags;
        }
    }
}
