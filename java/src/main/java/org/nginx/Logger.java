package org.nginx;

public class Logger implements Constants {
    protected Context context = null;

    public final void stderr(String message) {
        context.log(NGX_LOG_STDERR, message);
    };

    public final void emerg(String message) {
        context.log(NGX_LOG_EMERG, message);
    };

    public final void alert(String message) {
        context.log(NGX_LOG_ALERT, message);
    };

    public final void crtit(String message) {
        context.log(NGX_LOG_CRIT, message);
    };

    public final void error(String message) {
        context.log(NGX_LOG_ERR, message);
    };

    public final void warn(String message) {
        context.log(NGX_LOG_WARN, message);
    };

    public final void notice(String message) {
        context.log(NGX_LOG_NOTICE, message);
    };

    public final void info(String message) {
        context.log(NGX_LOG_INFO, message);
    };

    public final void debug(String message) {
        if (DEBUG)
            context.log(NGX_LOG_DEBUG, message);
    };

    Logger(Context context) {
        this.context = context;
    }
}
