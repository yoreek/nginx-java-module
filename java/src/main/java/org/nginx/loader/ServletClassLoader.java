package org.nginx.loader;

import java.io.*;
import java.util.HashMap;
import java.net.URL;

import org.nginx.Context;
import org.nginx.Logger;
import org.nginx.Constants;

public class ServletClassLoader extends ClassLoader implements Constants {
    protected ClassLoader system = null;

    protected Context context = null;

    protected Logger logger = null;

    protected final HashMap<String, ResourceEntry> resourceEntries = new HashMap<String, ResourceEntry>();

    public ServletClassLoader(Context context) {
        this.context = context;

        this.logger = context.getLogger();

        system = getSystemClassLoader();

        if (DEBUG)
            logger.debug("Init servlet class loader");
    }

    public boolean modified() {
        for (ResourceEntry entry : resourceEntries.values()) {
            File file  = new File(entry.path);
            long lastModified = file.lastModified();

            if (lastModified != entry.lastModified) {
                if (DEBUG)
                    logger.info("Resource '" + entry.path
                                + "' was modified; Date is now: "
                                + new java.util.Date(lastModified) + " Was: "
                                + new java.util.Date(entry.lastModified));
                return true;
            }
        }
        return false;
    }

    public void destroy() {
        resourceEntries.clear();
    }

    private byte[] getBytes(String fileName) throws IOException {
        if (DEBUG)
            logger.debug("Read file " + fileName);

        File file = new File(fileName);
        long len = file.length();

        byte raw[] = new byte[(int) len];

        FileInputStream fin = new FileInputStream(file);

        int r = fin.read(raw);

        if (r != len)
            throw new IOException("Can't read file '" + fileName + "'");

        fin.close();

        return raw;
    }

    protected ResourceEntry findResourceInternal(String name) throws ClassNotFoundException {
        if (name == null)
            return null;

        ResourceEntry entry = resourceEntries.get(name);
        if (entry != null)
            return entry;

        String fileStub = name.replace( '.', File.separatorChar );
        URL url = getSystemResource(fileStub + ".java");

        if (url == null)
            throw new ClassNotFoundException("Class '" + name + "' not found");

        String javaFilename = url.getPath();
        String classFilename = javaFilename.substring(0, javaFilename.length() - "java".length()) + "class";

        File javaFile  = new File( javaFilename );
        File classFile = new File( classFilename );

        if (!classFile.exists() || javaFile.lastModified() > classFile.lastModified()) {
            try {
                if (!compile(javaFilename) || !classFile.exists()) {
                    throw new ClassNotFoundException("Compile failed of " + javaFilename);
                }
            } catch( IOException ie ) {
                throw new ClassNotFoundException(ie.toString());
            }
        }

        entry = new ResourceEntry();

        try {
            entry.binaryContent = getBytes(classFilename);
            entry.lastModified  = javaFile.lastModified();
            entry.name          = name;
            entry.path          = javaFilename;
        } catch(IOException ie) {
            throw new ClassNotFoundException(ie.toString());
        }

        resourceEntries.put(name, entry);

        return entry;
    }

    @Override
    public Class findClass(String name) throws ClassNotFoundException {
        if (DEBUG)
            logger.debug("Find class " + name);

        Class clazz = null;

        if (DEBUG)
            logger.debug("Try super.findClass()");

        try {
            clazz = super.findClass(name);
        } catch (ClassNotFoundException cnfe) {
        } catch (RuntimeException e) {
            throw e;
        }

        if (DEBUG)
            logger.debug("Try findClassInternal()");

        if (clazz == null) {
            try {
                clazz = findClassInternal(name);
            } catch(ClassNotFoundException cnfe) {
                throw cnfe;
            } catch (RuntimeException e) {
                throw e;
            }
        }

        return clazz;
    }

    public Class findClassInternal(String name) throws ClassNotFoundException {

        if (DEBUG)
            logger.debug("Find class " + name);

        if (!validate(name))
            throw new ClassNotFoundException(name);

        ResourceEntry entry = null;

        entry = findResourceInternal(name);

        if (entry == null)
            throw new ClassNotFoundException(name);

        Class clazz = entry.loadedClass;

        if (clazz != null)
            return clazz;

        if (DEBUG)
            logger.debug("Define class " + name);

        clazz = defineClass(name, entry.binaryContent, 0, entry.binaryContent.length);

        entry.loadedClass   = clazz;
        entry.binaryContent = null;

        return entry.loadedClass;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);

    }

    protected Class<?> findLoadedClass0(String name) {
        ResourceEntry entry = resourceEntries.get(name);
        if (entry != null) {
            return entry.loadedClass;
        }
        return null;

    }

    private boolean compile(String javaFile) throws IOException {
        if (DEBUG)
            logger.debug("Compiling " + javaFile);

        String[] cmd = new String[4];
        cmd[0] = "/usr/bin/javac";
        cmd[1] = "-classpath";
        cmd[2] = System.getProperty("java.class.path");
        cmd[3] = javaFile;

        Process p = Runtime.getRuntime().exec(cmd);

        try {
            p.waitFor();
        } catch(InterruptedException ie) {
            logger.error("Error to compile " + javaFile + " due: " + ie);
        }

        int ret = p.exitValue();

        return ret == 0;
    }

    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

        Class clazz = null;

        if (DEBUG)
            logger.debug("Load class " + name);

        clazz = findLoadedClass0(name);
        if (clazz != null) {
            if (resolve)
                resolveClass(clazz);
            return (clazz);
        }

        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (resolve)
                resolveClass(clazz);
            return (clazz);
        }

        try {
            clazz = findClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
            if (DEBUG)
                logger.debug("Class " + name + " not found: " + e);
        }

        try {
            clazz = system.loadClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        throw new ClassNotFoundException(name);
    }

    protected boolean validate(String name) {

        // Need to be careful with order here
        if (name == null) {
            // Can't load a class without a name
            return false;
        }
        if (name.startsWith("java.")) {
            // Must never load java.* classes
            return false;
        }
        if (name.startsWith("javax.servlet.jsp.jstl")) {
            // OK for web apps to package JSTL
            return true;
        }
        if (name.startsWith("javax.servlet.")) {
            // Web apps should never package any other Servlet or JSP classes
            return false;
        }
        if (name.startsWith("javax.el")) {
            // Must never load javax.el.* classes
            return false;
        }
        if (name.startsWith("nginx.")) {
            // Must never load nginx.* classes
            return false;
        }

        // Assume everything else is OK
        return true;

    }
}
