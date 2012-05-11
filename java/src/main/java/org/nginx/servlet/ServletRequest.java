package org.nginx.servlet;

public class ServletRequest {

    protected ServletContext context = null;

    public ServletRequest(ServletContext context) {
        this.context = context;
    }
}
