package ru.eaze.domain;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.indexes.EazeActionsIndex;
import ru.eaze.indexes.EazePathIndex;
import ru.eaze.settings.Settings;
import ru.eaze.util.EazeStormNotifications;
import ru.eaze.util.RegexpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EazeProjectStructure {

    private static final Pattern EAZE_URI_PATTERN = Pattern.compile("([a-z]+)://(.+)");

    /**
     * Returns the Eaze project structure for specified IntelliJ project if the given project contains {@code <web>} directory.
     *
     * @param project the object representing IntelliJ project whose Eaze structure to be returned
     * @return the Eaze structure for the specified project, or {@code null} if the structure cannot be constructed
     * @throws NullPointerException if the specified project is null
     */
    @Nullable
    public static EazeProjectStructure forProject(@NotNull Project project) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) {
            String webDirPath = Settings.forProject(project).getWebDir();
            VirtualFile webDir = baseDir.findFileByRelativePath(webDirPath);
            if (webDir != null) {
                return new EazeProjectStructure(project, webDir);
            }
        }
        return null;
    }

    final private Project project;
    final private VirtualFile webDir;

    private EazeProjectStructure(Project project, VirtualFile webDir) {
        this.project = project;
        this.webDir = webDir;
    }

    public boolean isPagesConfigFile(VirtualFile file) {
        return file != null && file.isValid() && file.equals(pagesFile());
    }

    public boolean isPagesConfigFile(PsiFile file) {
        return file != null && file.isValid() && file.equals(pagesXmlFile());
    }

    @Nullable
    private VirtualFile pagesFile() {
        return webDir.findFileByRelativePath("etc/conf/pages.xml");
    }

    @Nullable
    private VirtualFile pagesCacheFile() {
        VirtualFile cache = webDir.findFileByRelativePath("cache/");
        if (cache != null) {
            VirtualFile candidate = null;
            for (VirtualFile file : cache.getChildren()) {
                if (file.getName().startsWith("pages")) {
                    candidate = file;
                    break;
                }
            }

            if (Settings.forProject(project).isPagesCacheChecksumEnabled()) {
                VirtualFile pages = pagesFile();
                if (pages != null) {
                    try {
                        String md5 = checksum(pages);
                        String path = "pages_" + md5 + ".xml";
                        VirtualFile checkedCandidate = cache.findChild(path);
                        if (checkedCandidate == null && candidate != null) {
                            EazeStormNotifications.PAGES_CACHE_CHECKSUM_MISMATCH.show(project);
                        } else {
                            EazeStormNotifications.PAGES_CACHE_CHECKSUM_MISMATCH.expire(project);
                        }
                        return checkedCandidate;

                    } catch (IOException ex) {
                        return null;
                    }
                }
            } else {
                EazeStormNotifications.PAGES_CACHE_CHECKSUM_MISMATCH.expire(project);
            }
            return candidate;
        }
        return null;
    }

    private String checksum(VirtualFile file) throws IOException {
        InputStream input = null;
        try {
            input = file.getInputStream();
            return DigestUtils.md5Hex(input);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    @Nullable
    private XmlFile pagesXmlFile() {
        return xmlFile(pagesFile());
    }

    @Nullable
    private XmlFile pagesCacheXmlFile() {
        return xmlFile(pagesCacheFile());
    }

    private XmlFile xmlFile(VirtualFile file) {
        if (file != null && file.isValid()) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile instanceof XmlFile && psiFile.isValid()) {
                return (XmlFile) psiFile;
            }
        }
        return null;
    }

    public boolean isActionsConfigFile(VirtualFile file) {
        if (file == null || !file.isValid()) {
            return false;
        }
        VirtualFile libDir = webDir.findFileByRelativePath("lib/");
        VirtualFile libEazeDir = webDir.findFileByRelativePath("lib.eaze/");
        VirtualFile packageDir = file.getParent();
        if (packageDir != null && ((libDir != null && libDir.equals(packageDir.getParent()))
                                    || (libEazeDir != null && libEazeDir.equals(packageDir.getParent())))) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile instanceof XmlFile) {
                XmlFile xmlFile = (XmlFile) psiFile;
                return xmlFile.getRootTag() != null && xmlFile.getRootTag().getName().equals("actions");
            }
            return false;
        }
        return false;
    }

    @Nullable
    public EazeAction getActionByFullName(@NotNull String name) {
        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(EazeActionsIndex.NAME, name, projectScope());
        Iterator<VirtualFile> iterator = files.iterator();
        VirtualFile file = iterator.hasNext() ? iterator.next() : null;
        if (file != null) {
            List<Integer> offsets = FileBasedIndex.getInstance().getValues(EazeActionsIndex.NAME, name, GlobalSearchScope.fileScope(project, file));
            if (!offsets.isEmpty()) {
                int offset = offsets.get(0);
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile != null) {
                    PsiElement element = psiFile.findElementAt(offset);
                    XmlTag actionTag = element instanceof XmlTag ? (XmlTag) element :
                            (element != null && element.getParent() instanceof XmlTag ? (XmlTag) element.getParent() : null);
                    if (actionTag != null) {
                        VirtualFile actionFile = getFileByActionTag(actionTag);
                        return new EazeAction(name, actionTag, actionFile);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static VirtualFile getFileByActionTag(@NotNull XmlTag actionTag) {
        if (!actionTag.isValid()) {
            return null;
        }

        VirtualFile packageDir = actionTag.getContainingFile().getVirtualFile().getParent();
        VirtualFile actionsDir = packageDir.findFileByRelativePath("actions/");
        if (actionsDir == null) {
            return null;
        }

        XmlAttribute actionNameAttr = actionTag.getAttribute("name");
        String actionName = actionNameAttr != null ? actionNameAttr.getValue() : "";
        XmlTag pathTag = actionTag.findFirstSubTag("path");
        String path = "";
        if (pathTag != null) {
            path = pathTag.getValue().getTrimmedText();
        }
        if (path.isEmpty()) {
            path = actionName != null ? actionName : "";
        }
        if (path.isEmpty()) {
            return null;
        }
        path += ".php";
        return actionsDir.findFileByRelativePath(path);
    }

    public Collection<String>  getAvailableActionNames() {
        final Collection<String> names = new ArrayList<String>();
        Processor<String> processor = new Processor<String>() {
            @Override
            public boolean process(String key) {
            Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(EazeActionsIndex.NAME, key, projectScope());
            if (files.size() > 0) {
                names.add(key);
            }
            return true;
            }
        };
        FileBasedIndex.getInstance().processAllKeys(EazeActionsIndex.NAME, processor, project);
        return names;
    }

    public boolean isSitesConfigFile(VirtualFile file) {
        return file != null && file.isValid() && file.equals(sitesFile());
    }

    @Nullable
    private VirtualFile sitesFile() {
        return webDir.findFileByRelativePath("etc/conf/sites.xml");
    }

    public boolean isValidEazeUri(@NotNull String uri) {
        VirtualFile sitesFile = sitesFile();
        if (sitesFile == null || !sitesFile.isValid()) {
            return false;
        }
        Matcher matcher = EAZE_URI_PATTERN.matcher(uri);
        if (matcher.matches()) {
            String pathName = matcher.group(1);
            final Ref<Boolean> found = new Ref<Boolean>(false);
            FileBasedIndex.ValueProcessor<List<String>> processor = new FileBasedIndex.ValueProcessor<List<String>>() {
                @Override
                public boolean process(VirtualFile file, List<String> value) {
                    found.set(value.size() > 0);
                    return false;
                }
            };
            FileBasedIndex.getInstance().processValues(EazePathIndex.NAME, pathName, sitesFile, processor, projectScope());
            return found.get();
        }
        return false;
    }

    @Nullable
    public VirtualFile resolveEazeUri(@NotNull String uri) {
        VirtualFile sitesFile = sitesFile();
        if (sitesFile == null || !sitesFile.isValid()) {
            return null;
        }
        Matcher matcher = EAZE_URI_PATTERN.matcher(uri);
        if (matcher.matches()) {
            String pathName = matcher.group(1);
            String resource = matcher.group(2);

            final Ref<String> path = new Ref<String>();
            FileBasedIndex.ValueProcessor<List<String>> processor = new FileBasedIndex.ValueProcessor<List<String>>() {
                @Override
                public boolean process(VirtualFile file, List<String> value) {
                    if (!value.isEmpty()) {
                        path.set(value.get(0));     //first is OK, for now
                    }
                    return false;
                }
            };
            FileBasedIndex.getInstance().processValues(EazePathIndex.NAME, pathName, sitesFile, processor, projectScope());

            if (path.get() != null) {
                String resourcePath = path + "/" + resource;
                return webDir.findFileByRelativePath(resourcePath);
            }
        }
        return null;
    }

    @Nullable
    public VirtualFile localeDirectory() {
        if (!webDir.isValid()) {
            return null;
        }
        return webDir.findFileByRelativePath("etc/locale/");
    }

    @NotNull
    public GlobalSearchScope projectScope() {
        return GlobalSearchScopesCore.directoryScope(project, webDir, true);
    }


    //--------------TODO: REFACTOR
    // Following code has quite complex and hardly traceable logic and requires careful refactoring, should the need arise.
    // Currently this functionality is used exclusively by JumpToEazeAction and separated to LegacyLogic private inner class in order to avoid dependencies in main code.
    // LegacyLogic is initialized lazily in order not to affect main code performance.

    private LegacyLogic legacy;

    public Object[] getFileNamesForURL(String urlStr) {
        if (legacy == null) {
            legacy = new LegacyLogic();
        }
        return legacy.getFileNamesForURL(urlStr);
    }

    private class LegacyLogic {

        HashMap<String, EazeSite> sites = new HashMap<String, EazeSite>();
        EazeSite firstSite;
        HashMap<EazeSite.Host, List<EazePage>> pages = new HashMap<EazeSite.Host, List<EazePage>>();

        public LegacyLogic() {
            init();
        }

        private void init() {
            VirtualFile sitesXml = sitesFile();
            if (sitesXml == null || !sitesXml.isValid()) {
                EazeStormNotifications.MISSING_SITES_CONFIG.show(project);
                return;
            }
            EazeStormNotifications.MISSING_SITES_CONFIG.expire(project);
            readSitesXml(project, sitesXml);
        }

        private void analyzeSiteTag(XmlTag siteTag) {
            EazeSite site = new EazeSite(siteTag, sites);
            if (sites.isEmpty()) {
                firstSite = site;
            }
            sites.put(site.getName(), site);
        }

        private EazeSite.Host detectHost(String urlStr) {
            try {
                URL url = new URL(urlStr);
                for (EazeSite site : sites.values()) {
                    EazeSite.Host curHost = site.findHostByUrl(url);
                    if (curHost != null) {
                        return curHost;
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        private void readSitesXml(final Project project, VirtualFile file) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile != null) {
                XmlFile xml = (XmlFile) psiFile;
                XmlTag sitesTag = xml.getRootTag();
                if (sitesTag != null) {
                    XmlTag[] sitesTags = sitesTag.findSubTags("site");
                    for (XmlTag siteTag : sitesTags) {
                        analyzeSiteTag(siteTag);
                    }
                }
            }
        }

        private void loadPagesForHost(@NotNull EazeSite.Host host) {
            List<EazePage> pagesList = new LinkedList<EazePage>();

            XmlFile pagesXml = pagesXmlFile();
            if (pagesXml != null) {
                pagesList.addAll(listPagesForHost(host, pagesXml));
            }
            XmlFile pagesCacheXml = pagesCacheXmlFile();
            if (pagesCacheXml != null) {
                List<EazePage> cachedPages = listPagesForHost(host, pagesCacheXml);
                //pages uri resolution depends on the order in which pages are defined in pages.xml
                //that's why we need to place cache pages for dynamic uris approximately at the position they were supposed to be
                int prevIndex = -1;
                for (EazePage cachedPage : cachedPages) {
                    boolean found = false;
                    for (int i = 0; i < pagesList.size(); i++) {
                        EazePage page = pagesList.get(i);
                        if (StringUtils.equals(page.getTranslatedURI(), cachedPage.getTranslatedURI())) {
                            found = true;
                            prevIndex = i;
                            break;
                        }
                    }
                    if (!found) {
                        pagesList.add(++prevIndex, cachedPage);
                    }
                }
            }
            pages.put(host, pagesList);
        }

        @NotNull
        private List<EazePage> listPagesForHost(@NotNull EazeSite.Host host, @NotNull XmlFile pagesXml) {
            XmlTag sitesTag = pagesXml.getRootTag();
            if (sitesTag == null) {
                return Collections.emptyList();
            }
            String hostSiteName = host.getSite().getName();
            XmlTag[] siteTags = sitesTag.findSubTags("site");
            if (siteTags.length > 0) {
                XmlTag siteTag = siteTags[0];
                ArrayList<String> siteNames = new ArrayList<String>();
                String siteName = siteTag.getAttributeValue("name");
                if (siteName != null) {
                    siteNames.add(siteName);
                }
                String names = siteTag.getAttributeValue("names");
                if (names != null) {
                    String[] tempNames = trimStringArray(names.split(","));
                    siteNames.addAll(Arrays.asList(tempNames));
                }
                if (siteNames.contains(hostSiteName)) {
                    XmlTag hostsTag = siteTag.findFirstSubTag("hosts");
                    HashMap<String, String> chains = new HashMap<String, String>();
                    if (hostsTag != null) {
                        XmlTag[] allHostsTags = hostsTag.findSubTags("host");
                        for (XmlTag hostTag : allHostsTags) {
                            String hostName = hostTag.getAttributeValue("name");
                            if (hostName == null || (!hostName.equals(host.getName()) && !hostName.equals("*"))) {
                                continue;
                            }
                            XmlTag[] actionTags = hostTag.findSubTags("action");
                            for (XmlTag action : actionTags) {
                                String actionName = action.getAttributeValue("name");
                                try {
                                    String value = action.getValue().getText();
                                    chains.put(actionName, value);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                    XmlTag pagesTag = siteTag.findFirstSubTag("pages");
                    if (pagesTag != null) {
                        return listPages(host, pagesTag, "", "", chains);
                    }
                }
            }
            return Collections.emptyList();
        }

        private List<EazePage> listPages(@NotNull EazeSite.Host host, @NotNull XmlTag parentTag, @NotNull String inheritedBoot, @NotNull String inheritedShutdown, @NotNull Map<String, String> chains) {
            List<EazePage> pagesList = new ArrayList<EazePage>();

            XmlTag[] pagesOrPagesGroupsTags = parentTag.getSubTags();
            for (XmlTag tag : pagesOrPagesGroupsTags) {
                String tagName = tag.getName();

                String tagBoot = tag.getAttributeValue("boot");
                String boot = tagBoot == null ? inheritedBoot : tagBoot;

                String tagShutdown = tag.getAttributeValue("shutdown");
                String shutdown = tagShutdown == null ? inheritedShutdown : tagShutdown;

                if (tagName.equals("page")) {
                    String uri = tag.getAttributeValue("uri");
                    uri = uri == null ? "" : host.translateEazePath(uri);

                    XmlTag templateTag = tag.findFirstSubTag("template");
                    String templatePath = templateTag == null ? "" : templateTag.getValue().getText();
                    String translatedPath = host.translateEazePath(templatePath);

                    XmlTag actionsTag = tag.findFirstSubTag("actions");
                    String actionString = actionsTag == null ? "" : actionsTag.getValue().getText();

                    String[] pageActions = actionNamesFromString(actionString, chains);
                    String[] bootActions = actionNamesFromString(boot, chains);
                    String[] shutdownActions = actionNamesFromString(shutdown, chains);
                    VirtualFile file = webDir.findFileByRelativePath(translatedPath);

                    EazePage page = new EazePage(file, translatedPath, uri, pageActions, bootActions, shutdownActions);
                    pagesList.add(page);

                } else if (tagName.equals("pageGroup")) {
                    pagesList.addAll(listPages(host, tag, boot, shutdown, chains));
                }
            }

            return pagesList;
        }

        private String[] actionNamesFromString(@NotNull String actions, @NotNull Map<String, String> chains) {
            for (String chainName : chains.keySet()) {
                actions = actions.replace(chainName, chains.get(chainName));
            }
            return trimStringArray(actions.split(","));
        }

        private String[] trimStringArray(String[] input) {
            for (int i = 0; i < input.length; i++) {
                input[i] = input[i].trim();
            }
            return input;
        }

        private EazeSite.Host getFirstHost() {
            if (firstSite == null) {
                return null;
            }
            return firstSite.getFirstHost();
        }

        public Object[] getFileNamesForURL(String urlStr) {
            if (urlStr == null || urlStr.isEmpty()) {
                return new Object[0];
            }

            ArrayList<MyListElement> elements = new ArrayList<MyListElement>();
            try {
                urlStr = URLDecoder.decode(urlStr, "utf-8");
            } catch (UnsupportedEncodingException ignored) {
            }
            URL url = null;
            try {
                url = new URL(urlStr);
            } catch (Exception ignored) {
            }
            if (url == null || !url.getProtocol().contains("http")) {
                EazeSite.Host tempHost = getFirstHost();
                if (tempHost != null) {
                    String path = tempHost.translateEazePath(urlStr);
                    urlStr = "http://localhost" + path;
                    try {
                        url = new URL(urlStr);
                    } catch (Exception ex) {
                        return new Object[0];
                    }
                }

            }
            EazeSite.Host host = detectHost(urlStr);

            if (host == null) {
                host = getFirstHost();
            }
            if (host != null) {
                if (!pages.containsKey(host)) {
                    loadPagesForHost(host);
                }
                String path = host.getPagePathByURL(url);
                List<EazePage> hostPages = pages.get(host);
                if (hostPages != null) {
                    for (EazePage page : hostPages) {
                        boolean matched;
                        try {
                            String regexp = "{^(" + page.getTranslatedURI() + ")(\\?(?:.*)|$)}i";
                            matched = page.getTranslatedURI().equals(path) || RegexpUtils.preg_match(regexp, path, null);
                        } catch (Exception ex) {
                            continue;
                        }

                        if (matched) {
                            elements.addAll(listActionElements(page.getActions(), "action"));
                            elements.addAll(listTemplateElements(page.getTemplatePath(), page.getFile(), host));
                            elements.addAll(listActionElements(page.getBootActions(), "boot"));
                            elements.addAll(listActionElements(page.getShutdownActions(), "shutdown"));
                            return elements.toArray();
                        }
                    }
                }
            }
            return elements.toArray();
        }

        private List<MyListElement> listActionElements(String[] actions, @NotNull String description) {
            List<MyListElement> elements = new ArrayList<MyListElement>();
            for (String fullActionName : actions) {
                EazeAction action = getActionByFullName(fullActionName);
                if (action != null) {
                    VirtualFile file = action.getFile();
                    if (file != null) {
                        String fileName = action.getFile().getPath();
                        MyListElement el = new MyListElement(fileName, file, description, fullActionName);
                        elements.add(el);
                    }
                }
            }
            return elements;
        }

        private List<MyListElement> listTemplateElements(String templateName, VirtualFile templateFile, @NotNull EazeSite.Host host) {
            List<MyListElement> elements = new ArrayList<MyListElement>();

            if (templateName != null && !templateName.isEmpty() && templateFile != null) {
                MyListElement el = new MyListElement(templateName, templateFile, "tmpl", "");
                elements.add(el);

                PsiFile psiTemplate = PsiManager.getInstance(project).findFile(templateFile);
                String text = psiTemplate != null ? psiTemplate.getText() : "";
                List<List<String>> rez = new ArrayList<List<String>>();
                RegexpUtils.preg_match_all("/\\{increal:(.+?)\\}/", text, rez);

                for (List<String> aRez : rez) {
                    String templatePath = aRez.get(1);
                    if (templatePath != null) {
                        String translatedPath = host.translateEazePath(templatePath);
                        templateFile = webDir.findFileByRelativePath(translatedPath);
                        if (templateFile != null) {
                            MyListElement _el = new MyListElement(templateFile.getName(), templateFile, "tmpl", "", true);
                            elements.add(_el);
                        }
                    }
                }
            }
            return elements;
        }
    }
}
