#include "ngx_http_java_core.h"

#define JAVA_CONTEXT_CLASS  "org/nginx/Context"

JNIEXPORT void JNICALL ngx_http_java_context_main_log
  (JNIEnv *jenv, jobject jobj, jint level, jstring message)
{
    ngx_str_t           str;

    str.len   = (*jenv)->GetStringUTFLength(jenv, message);
    str.data  = (u_char *) (*jenv)->GetStringUTFChars(jenv, message, 0);

    ngx_log_error_core((ngx_uint_t) level, ngx_cycle->log, 0,
            "%V", &str);
}

static JNINativeMethod nativeMethods[] = {
    {"native_main_log", "(ILjava/lang/String;)V", (void *)&ngx_http_java_context_main_log},
};

int ngx_http_java_context_init(JNIEnv *jenv)
{
    jclass clsH;

    clsH = (*jenv)->FindClass(jenv, JAVA_CONTEXT_CLASS);

    if (clsH == NULL) {
        ngx_log_error(NGX_LOG_ERR, ngx_cycle->log, 0,
                      "Unable to find class \"%s\"", JAVA_CONTEXT_CLASS);
        return 0;
    }

    int numberMethods = sizeof(nativeMethods) / sizeof(nativeMethods[0]);
    (*jenv)->RegisterNatives(jenv, clsH, nativeMethods, numberMethods);

    return 1;
}
