#ifndef _NGX_HTTP_JAVA_CORE_H_INCLUDED_
#define _NGX_HTTP_JAVA_CORE_H_INCLUDED_

#include <stdio.h>
#include <string.h>
#include <sys/un.h>
#include <sys/stat.h>
#include <time.h>
#include <jni.h>

#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>

int ngx_http_java_context_init(JNIEnv *jenv);
int ngx_http_java_servlet_context_init(JNIEnv *jenv);

#endif /* _NGX_HTTP_JAVA_CORE_H_INCLUDED_ */
