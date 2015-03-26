package ru.eaze.domain;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.indexes.EazeActionsIndex;
import ru.eaze.indexes.EazePathIndex;
import ru.eaze.settings.Settings;
import ru.eaze.util.RegexpUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
        if (file == null || !file.isValid()) {
            return false;
        }
        VirtualFile pagesFile = getPagesFile();
        return file.equals(pagesFile);
    }

    public boolean isPagesConfigFile(PsiFile file) {
        if (file == null || !file.isValid()) {
            return false;
        }
        PsiFile pagesFile = getPagesXmlFile();
        return file.equals(pagesFile);
    }

    @Nullable
    private VirtualFile getPagesFile() {
        return webDir.findFileByRelativePath("etc/conf/pages.xml");
    }

    @Nullable
    private XmlFile getPagesXmlFile() {
        VirtualFile pagesFile = getPagesFile();
        if (pagesFile != null && pagesFile.isValid()) {
            PsiFile pagesXml = PsiManager.getInstance(project).findFile(pagesFile);
            if (pagesXml instanceof XmlFile && pagesXml.isValid()) {
                return (XmlFile) pagesXml;
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
        if (file == null || !file.isValid()) {
            return false;
        }
        VirtualFile sitesFile = getSitesFile();
        return file.equals(sitesFile);
    }

    @Nullable
    private VirtualFile getSitesFile() {
        return webDir.findFileByRelativePath("etc/conf/sites.xml");
    }

    public boolean isValidEazeUri(@NotNull String uri) {
        VirtualFile sitesFile = getSitesFile();
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
        VirtualFile sitesFile = getSitesFile();
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
        HashMap<EazeSite.Host, ArrayList<EazePage>> pages = new HashMap<EazeSite.Host, ArrayList<EazePage>>();
        HashMap<String, String> currentChains = new HashMap<String, String>();

        public LegacyLogic() {
            init();
        }

        private void init() {
            VirtualFile sitesXml = getSitesFile();
            if (sitesXml == null || !sitesXml.isValid()) {
                Notifications.Bus.notify(new Notification("EazeStorm", "Eaze project error", "Missing file etc/conf/sites.xml in web directory", NotificationType.ERROR));
                return;
            }
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
                        this.analyzeSiteTag(siteTag);
                    }
                }
            }
        }

        private void loadPagesForHost(EazeSite.Host host) {
            currentChains.clear();
            XmlFile pagesXml = getPagesXmlFile();
            if (pagesXml == null) {
                return;
            }
            XmlTag sitesTag = pagesXml.getRootTag();
            String hostSiteName = host.getSite().getName();
            if (sitesTag == null) {
                return;
            }
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
                                    currentChains.put(actionName, value);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                    XmlTag pagesTag = siteTag.findFirstSubTag("pages");
                    this.analyzePagesGroupsTags(host, pagesTag, "", "");
                }
            }
        }

        private void analyzePagesGroupsTags(EazeSite.Host host, XmlTag parentTag, String inheritedBoot, String inheritedShutdown) {
            XmlTag[] pagesOrPagesGroupsTags = parentTag.getSubTags();
            for (XmlTag tag : pagesOrPagesGroupsTags) {
                String boot = inheritedBoot;
                String shutdown = inheritedShutdown;
                String tagName = tag.getName();

                String currentTagBoot = tag.getAttributeValue("boot");
                String currentTagShutdown = tag.getAttributeValue("shutdown");
                if (currentTagBoot != null) {
                    boot = currentTagBoot;
                }

                if (currentTagShutdown != null) {
                    shutdown = currentTagShutdown;
                }
                if (tagName.equals("page")) {
                    String uri = tag.getAttributeValue("uri");
                    uri = uri != null ? host.translateEazePath(uri) : "";
                    XmlTag templateTag = tag.findFirstSubTag("template");
                    XmlTag actionsTag = tag.findFirstSubTag("actions");
                    String templatePath = "";
                    String actionString = "";

                    if (templateTag != null) {
                        templatePath = templateTag.getValue().getText();
                    }

                    if (actionsTag != null) {
                        actionString = actionsTag.getValue().getText();
                    }

                    String translatedPath = host.translateEazePath(templatePath);

                    String[] pageActions = actionNamesFromString(actionString);
                    String[] bootActions = actionNamesFromString(boot);
                    String[] shutdownActions = actionNamesFromString(shutdown);
                    VirtualFile file = webDir.findFileByRelativePath(translatedPath);
                    EazePage page = new EazePage(file, translatedPath, uri, pageActions, bootActions, shutdownActions);
                    if (!pages.containsKey(host)) {
                        pages.put(host, new ArrayList<EazePage>());
                    }
                    pages.get(host).add(page);

                } else if (tagName.equals("pageGroup")) {
                    analyzePagesGroupsTags(host, tag, boot, shutdown);
                }
            }
        }

        private String[] actionNamesFromString(String actions) {
            for (String chainName : currentChains.keySet()) {
                actions = actions.replace(chainName, currentChains.get(chainName));
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
                ArrayList<EazePage> hostPages = pages.get(host);
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
                            String templateName = page.getTemplatePath();
                            String[] actions = page.getActions();
                            for (int i = actions.length - 1; i >= 0; i--) {
                                String fullActionName = actions[i];
                                EazeAction action = getActionByFullName(fullActionName);
                                if (action != null) {
                                    VirtualFile file = action.getFile();
                                    if (file != null) {
                                        String fileName = action.getFile().getPath();
                                        MyListElement el = new MyListElement(fileName, file, "action", fullActionName);
                                        elements.add(el);
                                    }
                                }
                            }

                            if (!templateName.isEmpty()) {
                                VirtualFile file = page.getFile();

                                if (file != null) {
                                    MyListElement el = new MyListElement(templateName, file, "tmpl", "");
                                    elements.add(el);

                                    PsiFile psiTemplate = PsiManager.getInstance(project).findFile(file);
                                    String text = psiTemplate != null ? psiTemplate.getText() : "";
                                    List<List<String>> rez = new ArrayList<List<String>>();
                                    RegexpUtils.preg_match_all("/\\{increal:(.+?)\\}/", text, rez);

                                    for (List<String> aRez : rez) {
                                        String templatePath = aRez.get(1);
                                        if (templatePath != null) {
                                            String translatedPath = host.translateEazePath(templatePath);
                                            VirtualFile templateFile = webDir.findFileByRelativePath(translatedPath);
                                            if (templateFile != null) {
                                                MyListElement _el = new MyListElement(templateFile.getName(), templateFile, "tmpl", "", true);
                                                elements.add(_el);
                                            }
                                        }
                                    }
                                }
                            }

                            actions = page.getBootActions();
                            for (String fullActionName : actions) {
                                EazeAction action = getActionByFullName(fullActionName);
                                if (action != null) {
                                    VirtualFile file = action.getFile();
                                    String fileName = action.getFile().getPath();
                                    MyListElement el = new MyListElement(fileName, file, "boot", fullActionName);
                                    elements.add(el);
                                }
                            }

                            actions = page.getShutdownActions();
                            for (String fullActionName : actions) {
                                EazeAction action = getActionByFullName(fullActionName);
                                if (action != null) {
                                    VirtualFile file = action.getFile();
                                    String fileName = action.getFile().getPath();
                                    MyListElement el = new MyListElement(fileName, file, "shutdown", fullActionName);
                                    elements.add(el);
                                }
                            }
                            return elements.toArray();
                        }
                    }
                }
            }
            return elements.toArray();
        }

    }
}
