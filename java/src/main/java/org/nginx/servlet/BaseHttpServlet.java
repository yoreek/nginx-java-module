package org.nginx.servlet;

import java.io.IOException;

import org.nginx.Constants;

public class BaseHttpServlet implements HttpServlet, Constants {
    private static final String METHOD_DELETE  = "DELETE";
    private static final String METHOD_HEAD    = "HEAD";
    private static final String METHOD_GET     = "GET";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String METHOD_POST    = "POST";
    private static final String METHOD_PUT     = "PUT";
    private static final String METHOD_TRACE   = "TRACE";

    public int init() {
        // System.out.println("NginxHttpServlet: INIT");
        return NGX_DONE;
    }

    public int service(ServletRequest request, ServletResponse response)  throws IOException {
        // System.out.println("HttpServlet: SERVICE");
        return NGX_DONE;
    }

    public void destroy() {
        // System.out.println("HttpServlet: DESTROY");
    }

    public BaseHttpServlet(){
        // System.out.println("HttpServlet: NEW");
    }

    public void finalize() {
        // System.out.println("A garbage collected");
    }
}
