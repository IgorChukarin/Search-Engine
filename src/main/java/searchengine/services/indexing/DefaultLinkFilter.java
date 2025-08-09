package searchengine.services.indexing;

import searchengine.model.Site;

import java.net.URI;
import java.net.URISyntaxException;

public class DefaultLinkFilter implements LinkFilter{

    @Override
    public boolean shouldFilter(String link, Site site) {
        String lowerCaseLink = link.toLowerCase();
        return isAnchorOrScript(lowerCaseLink) || isTelOrMailto(lowerCaseLink) || isImage(lowerCaseLink)
                || isFile(lowerCaseLink) || isAnotherSite(link, site) || hasExternalRedirect(lowerCaseLink);
    }


    private boolean isAnchorOrScript(String link) {
        return link.contains("#") || link.startsWith("javascript:");
    }


    private boolean isTelOrMailto(String link) {
        return link.startsWith("tel:") || link.startsWith("mailto:");
    }


    private boolean isImage(String link) {
        return link.endsWith(".jpg") || link.endsWith(".jpeg") || link.endsWith(".png")
                || link.endsWith(".gif") || link.endsWith(".bmp") || link.endsWith(".svg")
                || link.endsWith(".webp") || link.endsWith(".tiff") || link.endsWith(".ico");
    }


    private boolean isFile(String link) {
        return link.endsWith(".pdf") || link.endsWith(".doc") || link.endsWith(".docx")
                || link.endsWith(".xls") || link.endsWith(".xlsx") || link.endsWith(".zip")
                || link.endsWith(".rar") || link.endsWith(".7z") || link.endsWith(".tar")
                || link.endsWith(".gz") || link.endsWith(".exe") || link.endsWith(".mp3")
                || link.endsWith(".mp4") || link.endsWith(".avi") || link.endsWith(".mov");
    }


    private boolean isAnotherSite(String link, Site site) {
        try {
            URI linkUri = new URI(link);
            URI rootUri = new URI(site.getUrl());
            String linkHost = linkUri.getHost();
            String rootHost = rootUri.getHost();
            if (linkHost == null) {
                return false;
            }
            return !linkHost.equalsIgnoreCase(rootHost);
        } catch (URISyntaxException e) {
            return true;
        }
    }


    private boolean hasExternalRedirect(String link) {
        try {
            URI uri = new URI(link);
            String query = uri.getQuery();
            if (query != null && (query.contains("http://") || query.contains("https://"))) {
                return true;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        return false;
    }
}
