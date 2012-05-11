package org.nginx.servlet;

import org.nginx.Constants;

import java.io.PrintWriter;

public class ServletResponse implements Constants {

    protected ServletContext context = null;

    protected int status = 200;

    protected String message = null;

    protected BufferedWriter writer = null;

    private boolean isCharacterEncodingSet = false;

    protected boolean commited = false;

    protected boolean usingWriter = false;

    protected String contentType = null;

    protected String characterEncoding = DEFAULT_CHARACTER_ENCODING;

    protected long contentLength = -1;

    protected boolean charsetSet = false;

    public int getStatus() {
        return status;
    }

    public void setStatus( int status ) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCommitted() {
        return commited;
    }

    public void setCommitted(boolean v) {
        this.commited = v;
    }

    public void finishResponse() {
        // Writing leftover bytes
        writer.close();
    }

    public BufferedWriter getWriter() {
        usingWriter = true;
        if (writer == null) {
            writer = new BufferedWriter(context);
        }
        return writer;
    }

    public void setCharacterEncoding(String charset) {

        if (commited) {
            return;
        }

        // Ignore any call made after the getWriter has been invoked
        // The default should be used
        if (usingWriter) {
            return;
        }

        if (charset == null)
            return;

        characterEncoding = charset;
        isCharacterEncodingSet = true;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public ServletResponse(ServletContext context) {
        this.context = context;
        writer = new BufferedWriter(context);
    }
}
