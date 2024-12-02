package ch.admin.bit.jeap.starter.application.web;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Set;

public class FrontendRouteMatcher {

    public static boolean mightBeFrontendRouteWithIgnoreList(WebRequest webRequest, Set<String> nonFrontendRootPathParts) {
        if (webRequest instanceof ServletWebRequest servletRequest && servletRequest.getRequest().getServletPath() != null) {
            String path = servletRequest.getRequest().getServletPath();
            return mightBeFrontendPathWithIgnoreList(path, nonFrontendRootPathParts);
        }
        return false;
    }

    private static boolean mightBeFrontendPathWithIgnoreList(String path, Set<String> nonFrontendRootPathParts) {
        // Does the request look like a file with a dot extension?
        boolean hasFileExtension = path.contains(".");
        if (hasFileExtension) {
            return false;
        }

        // Does the request contain a known non-frontend path part such as api, actuator, ...
        String[] pathSegments = path.replaceFirst("^/", "").split("/", 2);
        String rootPath = pathSegments[0].toLowerCase();

        return nonFrontendRootPathParts.stream()
                .noneMatch(rootPath::contains);
    }
}
