package org.nginx.loader;

import java.net.URL;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

public class ResourceEntry {
    public String name = null;

    public String path = null;

    public long lastModified = -1;

    public byte[] binaryContent = null;

    public volatile Class<?> loadedClass = null;
}

