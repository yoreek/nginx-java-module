package org.nginx;

public interface Constants {
    public static final boolean DEBUG     =  true;

    public static final int NGX_OK        =  0;
    public static final int NGX_ERROR     = -1;
    public static final int NGX_AGAIN     = -2;
    public static final int NGX_BUSY      = -3;
    public static final int NGX_DONE      = -4;
    public static final int NGX_DECLINED  = -5;
    public static final int NGX_ABORT     = -6;

    public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    public static final int NGX_LOG_STDERR            = 0;
    public static final int NGX_LOG_EMERG             = 1;
    public static final int NGX_LOG_ALERT             = 2;
    public static final int NGX_LOG_CRIT              = 3;
    public static final int NGX_LOG_ERR               = 4;
    public static final int NGX_LOG_WARN              = 5;
    public static final int NGX_LOG_NOTICE            = 6;
    public static final int NGX_LOG_INFO              = 7;
    public static final int NGX_LOG_DEBUG             = 8;

    public static final int NGX_LOG_DEBUG_CORE        = 0x010;
    public static final int NGX_LOG_DEBUG_ALLOC       = 0x020;
    public static final int NGX_LOG_DEBUG_MUTEX       = 0x040;
    public static final int NGX_LOG_DEBUG_EVENT       = 0x080;
    public static final int NGX_LOG_DEBUG_HTTP        = 0x100;
    public static final int NGX_LOG_DEBUG_MAIL        = 0x200;
    public static final int NGX_LOG_DEBUG_MYSQL       = 0x400;
}
