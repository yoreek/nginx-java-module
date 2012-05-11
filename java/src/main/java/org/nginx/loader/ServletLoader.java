package org.nginx.loader;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.nginx.servlet.HttpServlet;
import org.nginx.Context;
import org.nginx.Logger;
import org.nginx.Constants;

public class ServletLoader implements Constants {
    protected Context context = null;

    protected Logger logger = null;

    private ServletClassLoader classLoader = null;

    private final Map<String, HttpServlet> servlets = new HashMap<String, HttpServlet>();

    private String loaderClass =
        "org.nginx.loader.ServletClassLoader";

    private boolean reloadable = true;

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getLoaderClass() {
        return this.loaderClass;
    }

    public void setLoaderClass(String loaderClass) {
        this.loaderClass = loaderClass;
    }

    public boolean getReloadable() {
        return this.reloadable;
    }

    private HttpServlet createServlet(Context context, String className) {
        if (DEBUG)
            logger.debug("Create servlet instance of " + className);

        HttpServlet httpServlet = null;

        try {
            Class clazz  = classLoader.loadClass(className);
            httpServlet = (HttpServlet) clazz.newInstance();
        }
        catch ( ClassNotFoundException ex ){
            logger.error( ex + " Interpreter class must be in class path.");
        }
        catch( InstantiationException ex ){
            logger.error( ex + " Interpreter class must be concrete.");
        }
        catch( IllegalAccessException ex ){
            logger.error( ex + " Interpreter class must have a no-arg constructor.");
        }

        return httpServlet;
    }

    public HttpServlet getServlet(String className) {
        if (DEBUG)
            logger.debug("Get servlet of " + className);

        HttpServlet httpServlet = servlets.get(className);

        if (httpServlet == null) {
            httpServlet = createServlet(context, className);
            httpServlet.init();
            servlets.put(className, httpServlet);
        }

        return httpServlet;
    }

    public ServletLoader(Context context) {
        this.context = context;
        logger  = context.getLogger();

        if (DEBUG)
            logger.debug("Init servlet loader");

        try {
            classLoader = createClassLoader();
        } catch (Exception e) {
            logger.error("Error to create class loader: " + e);
        }
    }

    private ServletClassLoader createClassLoader() throws Exception {

        Class<?> clazz = Class.forName(loaderClass);
        ServletClassLoader classLoader = null;

        Constructor ctor = clazz.getDeclaredConstructor(Context.class);
        ctor.setAccessible(true);
        classLoader = (ServletClassLoader) ctor.newInstance(context);

        return classLoader;

    }

    public void backgroundProcess() {
        if (DEBUG)
            logger.debug("Run background process");

        if (reloadable && modified()) {
            if (DEBUG)
                logger.debug("Modified");

            try {
                for(HttpServlet servlet : servlets.values())
                    servlet.destroy();
                servlets.clear();

                classLoader.destroy();

                classLoader = createClassLoader();
            } catch (Exception e) {
            }
        }
    }

    public boolean modified() {
        return classLoader != null ? classLoader.modified() : false;
    }
}
