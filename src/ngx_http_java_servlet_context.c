#include "ngx_http_java_core.h"

#define JAVA_SERVLET_CONTEXT_CLASS  "org/nginx/servlet/ServletContext"

JNIEXPORT void JNICALL ngx_http_java_servlet_context_log
  (JNIEnv *jenv, jobject jobj, jlong env, jint level, jstring message)
{
    ngx_http_request_t *r = (ngx_http_request_t *)env;
    ngx_str_t           str;

    str.len   = (*jenv)->GetStringUTFLength(jenv, message);
    str.data  = (u_char *) (*jenv)->GetStringUTFChars(jenv, message, 0);

    ngx_log_error((ngx_uint_t) level, r->connection->log, 0,
            "%V", &str);
}


JNIEXPORT void JNICALL ngx_http_java_servlet_context_write
  (JNIEnv *jenv, jobject jobj, jlong env, jbyteArray bytes, int offset, int len)
{
    ngx_buf_t    *b;
    ngx_chain_t   out;
    ngx_http_request_t *r = (ngx_http_request_t *)env;

    b = ngx_pcalloc(r->pool, sizeof(ngx_buf_t));
    if (b == NULL) {
        ngx_log_error(NGX_LOG_ERR, r->connection->log, 0,
            "ngx_http_java_servlet_context_write: Failed to allocate response buffer.");
        return;
    }

    b->pos = ngx_pnalloc(r->pool, len);
    if (b->pos == NULL) {
        ngx_log_error(NGX_LOG_ERR, r->connection->log, 0,
            "ngx_http_java_servlet_context_write: Failed to allocate response buffer.");
        return;
    }

    (*jenv)->GetByteArrayRegion(jenv, bytes, offset, len,
                                    (jbyte *) b->pos);

    b->last = b->pos + len;

    b->memory = 1;

    b->last_buf = 0;

    out.buf = b;
    out.next = NULL;

    if (!r->header_sent) {
        r->headers_out.status = NGX_HTTP_OK;
        r->headers_out.content_length_n = -1;
        r->headers_out.content_type.len = sizeof("text/html") - 1;
        r->headers_out.content_type.data = (u_char *) "text/html";
        ngx_http_send_header(r);
    }

    ngx_http_output_filter(r, &out);
}

JNIEXPORT void JNICALL ngx_http_java_servlet_context_finalize_request
  (JNIEnv *jenv, jobject jobj, jlong env)
{
    ngx_http_request_t *r = (ngx_http_request_t *)env;

    if (!r->header_sent) {
        r->headers_out.status = NGX_HTTP_OK;
        r->headers_out.content_length_n = -1;
        r->headers_out.content_type.len = sizeof("text/html") - 1;
        r->headers_out.content_type.data = (u_char *) "text/html";
        ngx_http_send_header(r);
    }

    ngx_http_send_special(r, NGX_HTTP_LAST);
}

static JNINativeMethod nativeMethods[] = {
    {"native_log",              "(JILjava/lang/String;)V", (void *)&ngx_http_java_servlet_context_log},
    {"native_write",            "(J[BII)V",                (void *)&ngx_http_java_servlet_context_write},
    {"native_finalize_request", "(J)V",                    (void *)&ngx_http_java_servlet_context_finalize_request},
};

int ngx_http_java_servlet_context_init(JNIEnv *jenv)
{
    jclass clsH;

    clsH = (*jenv)->FindClass(jenv, JAVA_SERVLET_CONTEXT_CLASS);

    if (clsH == NULL) {
        ngx_log_error(NGX_LOG_ERR, ngx_cycle->log, 0,
                      "Unable to find class \"%s\"", JAVA_SERVLET_CONTEXT_CLASS);
        return 0;
    }

    int numberMethods = sizeof(nativeMethods) / sizeof(nativeMethods[0]);
    (*jenv)->RegisterNatives(jenv, clsH, nativeMethods, numberMethods);

    return 1;
}
