package de.dfki.resc28.miniold.util;

import java.net.URI;
import java.net.URISyntaxException;

public class URIBuilder extends org.apache.http.client.utils.URIBuilder {
    public URIBuilder() {
    }

    public URIBuilder(String string) throws URISyntaxException {
        super(string);
    }

    public URIBuilder(URI uri) {
        super(uri);
    }

    public URIBuilder addPath(String subPath) {
        if (subPath == null || subPath.isEmpty() || "/".equals(subPath)) {
            return this;
        }
        return setPath(appendSegmentToPath(getPath(), subPath));
    }

    private String appendSegmentToPath(String path, String segment) {
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        if (path.charAt(path.length() - 1) == '/' || segment.startsWith("/")) {
            return path + segment;
        }

        return path + "/" + segment;
    }
    
    public URIBuilder setPath(String path)
    {
    	return ((URIBuilder) super.setPath(path));
    }
}