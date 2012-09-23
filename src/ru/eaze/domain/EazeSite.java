package ru.eaze.domain;

import com.intellij.psi.xml.XmlTag;
import ru.eaze.util.RegexpUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 03.12.11
 * Time: 15:14
 * To change this template use File | Settings | File Templates.
 */
public class EazeSite {

    private String name;
    private String extendsSiteName;
    private Settings settings;
    private HashMap<String, Host> hosts = new HashMap<String, Host>();
    private Host firstHost = null;

    EazeSite(XmlTag siteTag, HashMap<String, EazeSite> allSites) {
        settings = new Settings(siteTag);

        name = siteTag.getAttributeValue("name");
        extendsSiteName = siteTag.getAttributeValue("extends");
        if (extendsSiteName != null) {
            EazeSite parentSite = allSites.get(extendsSiteName);
            if (parentSite != null) {
                settings.mergeWithInheritedSettings(parentSite.getSettings());
            }
        }

        XmlTag hostsTag = siteTag.findFirstSubTag("hosts");
        if (hostsTag != null) {
            XmlTag[] hostTags = hostsTag.findSubTags("host");
            for (XmlTag hostTag : hostTags) {
                Host host = new Host(hostTag, this);
                if (hosts.isEmpty()) {
                    firstHost = host;
                }
                hosts.put(host.getName(), host);
            }
        }
    }

    Host getFirstHost() {
        return firstHost;
    }

    Settings getSettings() {
        return settings;
    }


    Host findHostByUrl(URL url) {
        for (Host host : hosts.values()) {
            if (host.DetectByURL(url)) {
                System.out.println("Host detected: " + host.getName());
                return host;
            }
        }
        return null;
    }

    String getName() {
        return name;
    }

    public class Settings {
        private Map<String, String> paths = new HashMap<String, String>();

        Settings(XmlTag siteTag) {
            XmlTag settingTag = siteTag.findFirstSubTag("settings");
            if (settingTag != null) {
                XmlTag pathsTag = settingTag.findFirstSubTag("paths");
                if (pathsTag != null) {
                    XmlTag[] pathTags = pathsTag.findSubTags("path");

                    for (XmlTag tag : pathTags) {
                        String pathName = tag.getAttribute("name").getValue();
                        String pathValue = tag.getAttribute("value").getValue();
                        paths.put(pathName + "://", pathValue);
                    }
                }
            }
        }

        boolean pathExists(String pathName) {
            return paths.containsKey(pathName);
        }

        String getPathValue(String path) {
            return paths.get(path);
        }

        void mergeWithInheritedSettings(Settings settings1) {
            for (String key : settings1.paths.keySet()) {
                if (!paths.containsKey(key)) {
                    paths.put(key, settings1.paths.get(key));
                }
            }

        }
    }

    public class Host {
        String name;
        ArrayList<String> hostNames = new ArrayList<String>();
        ArrayList<String> webRoots = new ArrayList<String>();
        ArrayList<String> ports = new ArrayList<String>();
        ArrayList<String> protocols = new ArrayList<String>();
        Settings settings;
        EazeSite site;

        Host(XmlTag hostTag, EazeSite _site) {
            site = _site;
            name = hostTag.getAttributeValue("name");
            for (XmlTag tag : hostTag.findSubTags("hostname")) {
                String hostName = tag.getValue().getText();
                System.out.println(hostName);
                hostNames.add(hostName);
            }

            for (XmlTag tag : hostTag.findSubTags("webroot")) {
                webRoots.add("/" + tag.getValue().getTrimmedText());
            }


            for (XmlTag tag : hostTag.findSubTags("port")) {
                ports.add(tag.getValue().getTrimmedText());
            }

            for (XmlTag tag : hostTag.findSubTags("protocol")) {
                protocols.add(tag.getValue().getTrimmedText());
            }

            /*if ( webRoots.isEmpty()) {
             webRoots.add("/");
         }   */

            if (ports.isEmpty()) {
                ports.add("80");
            }

            if (protocols.isEmpty()) {
                protocols.add("http");
            }

            settings = new Settings(hostTag);
            settings.mergeWithInheritedSettings(site.getSettings());
        }

        String getName() {
            return name;
        }

        EazeSite getSite() {
            return site;
        }

        Settings getSettings() {
            return settings;
        }


        String getPagePathByURL(URL url) {
            String path = url.getPath();
            for (String webRoot : webRoots) {
                if (path.startsWith(webRoot)) {
                    return path.substring(webRoot.length());
                }
            }
            return path;
        }

        public String translateEazePath(String path) {
            java.util.List<String> regs = new ArrayList<String>();
            if (RegexpUtils.preg_match("{(.+://*).*}i", path, regs)) {
                String pathName = regs.get(1);
                String pathValue = settings.getPathValue(pathName);
                if (pathName != null && pathValue != null) {
                    return path.replaceAll(pathName, pathValue + "/");
                }
            }
            return path;
        }

        boolean DetectByURL(URL url) {
            String hostname = url.getHost();
            if (name.equals("localhost")) {
                int i = 0;
            }
            int port = url.getPort();
            String protocol = url.getProtocol();
            if (port == -1) {
                port = url.getDefaultPort();
            }
            String urlStr = String.valueOf(port);
            String path = url.getPath();
            if (!hostNames.contains(hostname) || !ports.contains(urlStr) || !protocols.contains(protocol)) {
                return false;
            }
            if (webRoots.isEmpty()) {
                return true;
            }
            for (String webRoot : webRoots) {
                if (path.startsWith(webRoot)) {
                    return true;
                }
            }
            return false;
        }

    }
}


