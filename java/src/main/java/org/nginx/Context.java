package org.nginx;

public class Context {

    protected Logger logger;

    public Logger getLogger() {
        return logger;
    }

    public void log(int level, String message) {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();

        String fullClassName = ste[3].getClassName();
        String className     = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName    = ste[3].getMethodName();
        int lineNumber       = ste[3].getLineNumber();

        native_main_log(level, className + ":" + lineNumber + " " + methodName + "() - " + message);
    }

    public static native void native_main_log(int level, String message);

    public Context() {
        logger = new Logger(this);
    }
}
