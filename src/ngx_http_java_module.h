#ifndef _NGX_HTTP_JAVA_MODULE_H_INCLUDED_
#define _NGX_HTTP_JAVA_MODULE_H_INCLUDED_

#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>

#include "ngx_http_java_core.h"


typedef struct {
    ngx_str_t            filename;
    ngx_str_t            redirect_uri;
    ngx_str_t            redirect_args;
    ngx_http_request_t  *request;
    ngx_uint_t           done, next;         /* unsigned  done:1; */
} ngx_http_java_ctx_t;

extern ngx_module_t  ngx_http_java_module;

#endif
