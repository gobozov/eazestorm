package ru.eaze.domain;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.util.RegexpUtils;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by IntelliJ IDEA.
 * User: zenden
 * Date: 01.12.11
 * Time: 23:21
 */
public class EazeProjectStructure {

    private static final ConcurrentMap<Project,EazeProjectStructure> structures = new ConcurrentHashMap<Project, EazeProjectStructure>();

    /**
     * Returns the Eaze project structure for specified IntelliJ project if the given project contains {@code <web>} directory.
     *
     * @param project the object representing IntelliJ project whose Eaze structure to be returned
     * @return the Eaze structure for the specified project, or {@code null} if the structure cannot be constructed
     * @throws NullPointerException if the specified project is null
     */
    @Nullable
    public static EazeProjectStructure forProject(Project project) {
        EazeProjectStructure structure = structures.get(project);
        if(structure == null) {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir != null) {
                VirtualFile webDir = baseDir.findFileByRelativePath("web/");
                if (webDir != null) {
                    final EazeProjectStructure newStructure = new EazeProjectStructure(project, webDir);
                    structure = structures.putIfAbsent(project, newStructure);
                    if(structure == null) {
                        structure = newStructure;
                    }
                }
            }
        }
        return structure;
    }

    private Project curProject;
    private VirtualFile webDir;
    //private  = new ArrayList<ru.eaze.domain.EazePage>();
    HashMap<String, String> actions = new HashMap<String, String>();
    HashMap<String, EazePackage> packages = new HashMap<String, EazePackage>();
    HashMap<String, EazeSite> sites = new HashMap<String, EazeSite>();
    EazeSite firstSite = null;
    HashMap<EazeSite.Host, ArrayList<EazePage>> pages = new HashMap<EazeSite.Host, ArrayList<EazePage>>();
    HashMap<String, String> currentChains = new HashMap<String, String>();
    XmlFile pagesXml;

    private EazeProjectStructure(final Project project, VirtualFile projectWebDir) {
        curProject = project;
        webDir = projectWebDir;
        init();
    }

    public VirtualFile getWebDir() {
        return webDir;
    }
    //public Map<String, String> pathMap =  new HashMap<String, String>();

    public void analyzeSiteTag(XmlTag siteTag) {
        EazeSite site = new EazeSite(siteTag, sites);
        if (sites.isEmpty()) {
            firstSite = site;
        }

        sites.put(site.getName(), site);
        /*XmlTag settingTag = siteTag.findFirstSubTag("settings");
        if ( settingTag != null) {
            XmlTag pathsTag = settingTag.findFirstSubTag("paths");
            if ( pathsTag != null ) {
                XmlTag[] pathTags = pathsTag.findSubTags("path");

                for ( XmlTag tag : pathTags) {
                    String pathName  =  tag.getAttribute("name").getValue();
                    String pathValue = tag.getAttribute("value").getValue();
                    pathMap.put( pathName + "://", pathValue );
                }
            }
        }    */
    }

    EazeSite.Host detectHost(String urlStr) {
        String hostName = "";
        String path = "";
        int port = 80;
        String webRoot = "/";
        try {
            URL url = new URL(urlStr);
            hostName = url.getHost();
            path = url.getPath();
            port = url.getPort();


            for (EazeSite site : sites.values()) {
                EazeSite.Host curHost = site.findHostByUrl(url);
                if (curHost != null) {
                    return curHost;
                }
            }
        } catch (Exception ex) {

        }
        return null;
    }

    public void readSitesXml(final Project project, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            XmlFile xml = (XmlFile) psiFile;
            XmlTag sitesTag = xml.getRootTag();

            String siteNames = "";
            XmlTag detectedSiteTag = null;
            XmlTag[] sitesTags = sitesTag.findSubTags("site");
            for (XmlTag siteTag : sitesTags) {
                String siteName = siteTag.getAttribute("name").getValue();
                this.analyzeSiteTag(siteTag);
            }

        }
    }

    /**
     * @param actionTag xml tag action in actions.xml
     * @return
     */
    public static VirtualFile GetFileByActionTag(XmlTag actionTag) {
        PsiFile containingFile = actionTag.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null){
            return null;
        }
        String packageName = actionTag.getContainingFile().getName();
        packageName = packageName.replace(".xml", "");
        String actionName = actionTag.getAttribute("name").getValue().toString();
        String path = "";
        XmlTag pathTag = actionTag.findFirstSubTag("path");
        if (pathTag != null) {
            path = pathTag.getValue().getText();
        }
        if (path == "") {
            path = actionName;
        }
        path += ".php";
        path = "/actions/" + path;
        VirtualFile parentFile =  virtualFile.getParent();
        if ( parentFile == null ){
            return null;
        }
        return parentFile.findFileByRelativePath(path);
    }

    public void loadPagesForHost(EazeSite.Host host) {
        currentChains.clear();
        XmlTag sitesTag = pagesXml.getRootTag();
        //String siteNames = "";
        XmlTag detectedSiteTag = null;
        String hostSiteName = host.getSite().getName();
        XmlTag[] siteTags = sitesTag.findSubTags("site");
        for (XmlTag siteTag : siteTags) {
            ArrayList<String> siteNames = new ArrayList<String>();
            String siteName = siteTag.getAttribute("name").getValue();
            siteNames.add(siteName);
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
                        if (!hostName.equals(host.getName()) && !hostName.equals("*")) {
                            continue;
                        }
                        XmlTag[] actionTags = hostTag.findSubTags("action");
                        for (XmlTag action : actionTags) {
                            String actionName = action.getAttributeValue("name");
                            try {
                                String value = action.getValue().getText();
                                currentChains.put(actionName, value);
                            } catch (Exception ex) {
                            }
                        }
                    }
                }
                XmlTag pagesTag = siteTag.findFirstSubTag("pages");
                this.analyzePagesGroupsTags(host, pagesTag, "", "");
            }
            //detectedSiteTag = siteTag;
            break;
        }


    }

    public void analyzePagesGroupsTags(EazeSite.Host host, XmlTag parentTag, String inheritedBoot, String inheritedShutdown) {
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
                String uri = tag.getAttribute("uri").getValue().toString();
                uri = host.translateEazePath(uri);
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

    public String[] actionNamesFromString(String actions) {
        for (String chainName : currentChains.keySet()) {
            actions = actions.replace(chainName, currentChains.get(chainName));
        }
        String[] pageActions = trimStringArray(actions.split(","));
        return pageActions;
    }


    public void init() {
        VirtualFile pagesXmlFile = webDir.findFileByRelativePath("etc/conf/pages.xml");
        VirtualFile sitesXml = webDir.findFileByRelativePath("etc/conf/sites.xml");
        readSitesXml(curProject, sitesXml);
        PsiFile pagesXmlPsiFile = PsiManager.getInstance(curProject).findFile(pagesXmlFile);
        if (pagesXmlPsiFile != null) {
            XmlFile file = (XmlFile) pagesXmlPsiFile;
            pagesXml = file;
        }
    }

    public boolean LooksLikeEazeUri(String uri) {
        List<String> rez = new ArrayList<String>();
        RegexpUtils.preg_match("/([a-z]+:\\/\\/).+/", uri, rez);
        if (rez.size() != 0) {
            EazeSite.Host host = getFirstHost();
            String pathName = rez.get(1);
            return host.getSettings().pathExists(pathName);
        }
        return false;
    }

    void loadPackage(String packageName) {
        VirtualFile pagesXmlFile = webDir.findFileByRelativePath("lib/" + packageName + "/" + packageName + ".xml");
        if (pagesXmlFile == null) {
            return;
        }
        EazePackage newPackage = new EazePackage(packageName, curProject, pagesXmlFile, webDir);
        packages.put(packageName, newPackage);
    }

   public EazePackage getPackageByName(String packageName) {
        if (packages.containsKey(packageName)) {
            return packages.get(packageName);
        }
        loadPackage(packageName);
        return packages.get(packageName);
    }

    public EazeAction getActionByFullName(String name) {
        name = name.trim();
        String[] tokens = trimStringArray(name.split("\\."));
        if (tokens.length < 3) {
            return null;
        }
        // String.
        String packageName = StringUtils.join(Arrays.copyOfRange(tokens, 0, tokens.length - 1), '.');

        // tokens[0] + "." + tokens[1];
        EazePackage eazePackage = getPackageByName(packageName);
        if (eazePackage == null) {
            return null;
        }
        return eazePackage.getActionByName(tokens[2]);
    }

    String extractPackageName(String actionName) {
        actionName = actionName.trim();
        String[] tokens = actionName.split("\\.");
        if (tokens.length < 3) {
            return null;
        }
        return StringUtils.join(Arrays.copyOfRange(tokens, 0, tokens.length - 1), '.');
    }

    public  static String[] trimStringArray(String[] input) {
        for (int i = 0; i < input.length; i++) {
            input[i] = input[i].trim();
        }
        return input;
    }

    public  EazeSite.Host getFirstHost() {
        if (firstSite == null) {
            return null;
        }
        return firstSite.getFirstHost();
    }

    public String[] getAvailablePackageNames() {
        ArrayList<String> packageNames = new ArrayList<String>();
        VirtualFile libDir = webDir.findFileByRelativePath("lib/");
        if (libDir != null) {
            VirtualFile[] files = libDir.getChildren();
            for (VirtualFile file : files) {
                if (file.isDirectory()) {
                    packageNames.add(file.getName());
                }
            }
        }
        return packageNames.toArray(new String[packageNames.size()]);
    }

    public Object[] getFileNamesForURL(String urlStr, List<String> fileNames) {
        ArrayList<MyListElement> elements = new ArrayList<MyListElement>();
        try {

            String hostName = "";
            String path = "";
            URL url = null;
            try {
                url = new URL(urlStr);
                hostName = url.getHost();
                path = url.getPath();
            } catch (Exception ex) {


            }
            if (url == null || !url.getProtocol().contains("http")) {
                EazeSite.Host tempHost = getFirstHost();
                if (tempHost != null) {
                    path = tempHost.translateEazePath(urlStr);
                    urlStr = "http://localhost" + path;
                    try {
                        url = new URL(urlStr);
                        hostName = url.getHost();
                        path = url.getPath();
                    } catch (Exception ex) {
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
                //  if ( p)
                path = host.getPagePathByURL(url);
                ArrayList<EazePage> hostPages = pages.get(host);
                if (hostPages != null)
                    for (EazePage page : hostPages) {
                        java.util.List<String> regs = new ArrayList<String>();

                        String regexp = "{^(" + page.getTranslatedURI() + ")(\\?(?:.*)|$)}i";
                        if (RegexpUtils.preg_match(regexp, path, regs)
                                || page.getTranslatedURI().equals(path)   // сомнительно.. хм...
                                ) {
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

                                    PsiFile psiTemplate = PsiManager.getInstance(curProject).findFile(file);
                                    String text = psiTemplate.getText();
                                    List<List<String>> rez = new ArrayList<List<String>>();
                                    RegexpUtils.preg_match_all("/\\{increal:(.+?)\\}/", text, rez);

                                    for (int i = 0; i < rez.size(); i++) {
                                        String templatePath = rez.get(i).get(1);
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
        } catch (Exception ex) {
        }
        return elements.toArray();
    }

    @NotNull
    public Collection<VirtualFile> getLocaleFiles() {
        Collection<VirtualFile> files = new HashSet<VirtualFile>();
        VirtualFile dir = webDir.findFileByRelativePath("etc/locale/");
        if (dir != null && dir.isValid() && dir.isDirectory()) {
            for (VirtualFile file : dir.getChildren()) {
                if (!file.isDirectory() && file.isValid() && file.getFileType() == XmlFileType.INSTANCE) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public boolean isLocaleFile(VirtualFile file) {
        return getLocaleFiles().contains(file);
    }
}
