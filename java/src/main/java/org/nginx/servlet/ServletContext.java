package org.nginx.servlet;

import org.nginx.Context;

public class ServletContext extends Context {
    protected long r;

    public void log(int level, String message) {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();

        String fullClassName = ste[3].getClassName();
        String className     = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName    = ste[3].getMethodName();
        int lineNumber       = ste[3].getLineNumber();

        native_log(r, level, className + ":" + lineNumber + " " + methodName + "() - " + message);
    };

    public void write(byte[] bytes, int offset, int len) {
        native_write(r, bytes, offset, len);
    };

    public void finalizeRequest() {
        native_finalize_request(r);
    }

    public static native void native_log(long r, int level, String message);

    public static native void native_write(long r, byte[] bytes, int offset, int len);

    public static native void native_finalize_request(long r);

    public ServletContext(long r) {
        super();
        this.r = r;
    }
}
