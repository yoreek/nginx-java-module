package org.nginx.servlet;

import java.io.IOException;

public interface HttpServlet {
    public int init();

    public int service(ServletRequest request, ServletResponse response) throws IOException;

    public void destroy();
}
