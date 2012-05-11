package org.nginx;

import java.io.IOException;

import org.nginx.loader.ServletLoader;
import org.nginx.servlet.*;

public final class Dispatcher implements Constants {

    public static final Context context = new Context();

    public static final Logger logger = context.getLogger();

    public static final ServletLoader servletLoader = new ServletLoader(context);

    public static final int process(long r, String className) {
        int result = NGX_ERROR;

        ServletContext servletContext = new ServletContext(r);
        Logger servletLogger = servletContext.getLogger();

        if (DEBUG)
            servletLogger.debug("Process request: " + className);


        ServletRequest  servletRequest  = new ServletRequest(servletContext);
        ServletResponse servletResponse = new ServletResponse(servletContext);

        HttpServlet httpServlet = servletLoader.getServlet(className);

        if (httpServlet != null) {
            try {
                result = httpServlet.service(servletRequest, servletResponse);
            } catch (IOException ie) {
                result = NGX_ERROR;
            }
        }

        servletLoader.backgroundProcess();

        return result;

    }
}
