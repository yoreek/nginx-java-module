
/*
 * Copyright (c) Igor Sysoev, Yuriy Ustyushenko
 */

#include "ngx_http_java_module.h"


typedef struct {
    ngx_array_t         *vm_options;
    JNIEnv              *jenv;
    JavaVM              *jvm;
} ngx_http_java_main_conf_t;


typedef struct {
    ngx_str_t            handler_name;
    jstring              java_handler_name;
    ngx_str_t            dispatcher_name;
    jmethodID            dispatcher_method;
    jclass               dispatcher_class;
} ngx_http_java_loc_conf_t;


void ngx_http_java_handle_request(ngx_http_request_t *r);
static char *ngx_http_java(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
static char *ngx_http_java_dispatcher(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
static char *ngx_http_java_create_vm(ngx_http_request_t *r, ngx_http_java_main_conf_t *jmcf);
static void *ngx_http_java_create_main_conf(ngx_conf_t *cf);
static void *ngx_http_java_create_loc_conf(ngx_conf_t *cf);
static char *ngx_http_java_merge_loc_conf(ngx_conf_t *cf, void *parent, void *child);
static ngx_int_t ngx_http_java_postconfiguration(ngx_conf_t *cf);
static ngx_int_t ngx_http_java_init_process(ngx_cycle_t *cycle);
static void ngx_http_java_exit_process(ngx_cycle_t *cycle);

static ngx_command_t  ngx_http_java_commands[] = {

    { ngx_string("java_options"),
      NGX_HTTP_MAIN_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_str_array_slot,
      NGX_HTTP_MAIN_CONF_OFFSET,
      offsetof(ngx_http_java_main_conf_t, vm_options),
      NULL },

    { ngx_string("java_handler"),
      NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_HTTP_LIF_CONF|NGX_CONF_TAKE1,
      ngx_http_java,
      NGX_HTTP_LOC_CONF_OFFSET,
      0,
      NULL },

    { ngx_string("java_dispatcher"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_HTTP_LIF_CONF|NGX_CONF_TAKE1,
      ngx_http_java_dispatcher,
      NGX_HTTP_LOC_CONF_OFFSET,
      0,
      NULL },

      ngx_null_command
};


static ngx_http_module_t  ngx_http_java_module_ctx = {
    NULL,                                  /* preconfiguration */
    ngx_http_java_postconfiguration,       /* postconfiguration */

    ngx_http_java_create_main_conf,        /* create main configuration */
    NULL,                                  /* init main configuration */

    NULL,                                  /* create server configuration */
    NULL,                                  /* merge server configuration */

    ngx_http_java_create_loc_conf,         /* create location configuration */
    ngx_http_java_merge_loc_conf           /* merge location configuration */
};


ngx_module_t ngx_http_java_module = {
    NGX_MODULE_V1,
    &ngx_http_java_module_ctx,             /* module context */
    ngx_http_java_commands,                /* module directives */
    NGX_HTTP_MODULE,                       /* module type */
    NULL,                                  /* init master */
    NULL,                                  /* init module */
    ngx_http_java_init_process,            /* init process */
    NULL,                                  /* init thread */
    NULL,                                  /* exit thread */
    ngx_http_java_exit_process,            /* exit process */
    NULL,                                  /* exit master */
    NGX_MODULE_V1_PADDING
};


static ngx_int_t
ngx_http_java_handler(ngx_http_request_t *r)
{
    r->main->count++;

    ngx_http_java_handle_request(r);

    return NGX_DONE;
}

void
ngx_http_java_handle_request(ngx_http_request_t *r)
{
    ngx_int_t                   rc;
    ngx_str_t                   uri, args;
    ngx_http_java_ctx_t        *ctx;
    ngx_http_java_loc_conf_t   *jlcf;
    ngx_http_java_main_conf_t  *jmcf;

#ifdef NGX_DEBUG
    ngx_log_debug0(NGX_LOG_DEBUG_HTTP, r->connection->log, 0, "java handle request");
#endif

    ctx = ngx_http_get_module_ctx(r, ngx_http_java_module);

    if (ctx == NULL) {
        ctx = ngx_pcalloc(r->pool, sizeof(ngx_http_java_ctx_t));
        if (ctx == NULL) {
            rc = NGX_ERROR;
            goto done;
        }

        ngx_http_set_ctx(r, ctx, ngx_http_java_module);
    }

    jmcf = ngx_http_get_module_main_conf(r, ngx_http_java_module);

    jlcf = ngx_http_get_module_loc_conf(r, ngx_http_java_module);

    /* get Java VM */
    if (jmcf->jvm == NULL && ngx_http_java_create_vm(r, jmcf) != NGX_CONF_OK) {
        rc = NGX_ERROR;
        goto done;
    }

    /* get request dispatcher */
    if (jlcf->dispatcher_method == NULL) {
        jlcf->dispatcher_class = (*jmcf->jenv)->FindClass(jmcf->jenv, (char *) jlcf->dispatcher_name.data);
        if (jlcf->dispatcher_class == NULL) {
            ngx_log_error(NGX_LOG_ERR, r->connection->log, 0,
                               "unable to find class \"%V\"", &jlcf->dispatcher_name);
            rc = NGX_ERROR;
            goto done;
        }

        jlcf->dispatcher_method = (*jmcf->jenv)->GetStaticMethodID(
            jmcf->jenv, jlcf->dispatcher_class, "process", "(JLjava/lang/String;)I");

        if (jlcf->dispatcher_method == NULL) {
            ngx_log_error(NGX_LOG_ERR, r->connection->log, 0,
                               "unable to find \"process\" method of the \"%V\" class", &jlcf->dispatcher_name);
            rc = NGX_ERROR;
            goto done;
        }
    }

    /* get java handler name */
    if (jlcf->java_handler_name == NULL) {
        jlcf->java_handler_name = (*jmcf->jenv)->NewStringUTF(jmcf->jenv,
            (const char *) jlcf->handler_name.data);
    }

    /* process request dispatcher */
#ifdef NGX_DEBUG
    ngx_log_debug0(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                   "java dispatch request");
#endif
    rc = (*jmcf->jenv)->CallStaticIntMethod(jmcf->jenv, jlcf->dispatcher_class,
                                            jlcf->dispatcher_method, r, jlcf->java_handler_name);

    if (rc == NGX_DONE) {
#ifdef NGX_DEBUG
        ngx_log_debug0(NGX_LOG_DEBUG_HTTP, r->connection->log, 0,
                       "finalize");
#endif

        ngx_http_finalize_request(r, rc);

        return;
    }

    if (rc > 600) {
        rc = NGX_OK;
    }

    if (ctx->redirect_uri.len) {
        uri  = ctx->redirect_uri;
        args = ctx->redirect_args;
    } else {
        uri.len = 0;
    }
    ctx->filename.data    = NULL;
    ctx->redirect_uri.len = 0;

    if (ctx->done || ctx->next) {
        ngx_http_finalize_request(r, NGX_DONE);
        return;
    }

    if (uri.len) {
        ngx_http_internal_redirect(r, &uri, &args);
        ngx_http_finalize_request(r, NGX_DONE);
        return;
    }

    if (rc == NGX_OK || rc == NGX_HTTP_OK) {
        ngx_http_send_special(r, NGX_HTTP_LAST);
        ctx->done = 1;
    }

done:
    ngx_http_finalize_request(r, rc);
}


static char *
ngx_http_java(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
    ngx_http_java_loc_conf_t   *jlcf = conf;
    ngx_http_core_loc_conf_t   *clcf;
    ngx_str_t                  *value;

    value = cf->args->elts;

    if (jlcf->handler_name.data != NULL) {
        ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                           "duplicate java handler \"%V\"", &value[1]);
        return NGX_CONF_ERROR;
    }

    jlcf->handler_name.data = value[1].data;
    jlcf->handler_name.len  = value[1].len;

    clcf = ngx_http_conf_get_module_loc_conf(cf, ngx_http_core_module);
    clcf->handler = ngx_http_java_handler;

    return NGX_CONF_OK;
}


static char *
ngx_http_java_dispatcher(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
    ngx_http_java_loc_conf_t   *jlcf = conf;
    ngx_http_core_loc_conf_t   *clcf;
    ngx_str_t                  *value;
    size_t                      n;
    u_char                     *src;

    value = cf->args->elts;

    if (jlcf->dispatcher_name.data != NULL) {
        ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                           "duplicate java dispatcher \"%V\"", &value[1]);
        //return NGX_CONF_ERROR;
    }

    /* replace '.' to '/' */
    src = value[1].data;
    n   = value[1].len;
    while (n) {
        if (*src == '.') *src = '/';
        src++;
        n--;
    }

    jlcf->dispatcher_name.data = value[1].data;
    jlcf->dispatcher_name.len  = value[1].len;

    return NGX_CONF_OK;
}


static char *
ngx_http_java_create_vm(ngx_http_request_t *r, ngx_http_java_main_conf_t *jmcf)
{
    JavaVMInitArgs   vm_args;
    JavaVMOption    *options;

    vm_args.version            = JNI_VERSION_1_6;
    vm_args.options            = NULL;
    vm_args.ignoreUnrecognized = 0;
    vm_args.nOptions           = 0;

    if (jmcf->vm_options) {
        u_int      i;
        ngx_str_t *str;

        vm_args.options = ngx_pcalloc(r->connection->pool, sizeof(JavaVMOption) * jmcf->vm_options->nelts);
        if (vm_args.options == NULL) {
            return NGX_CONF_ERROR;
        }

        vm_args.nOptions = jmcf->vm_options->nelts;
        str              = jmcf->vm_options->elts;

        for (i = 0; i < jmcf->vm_options->nelts; i++) {
            vm_args.options[i].optionString = (char *) str[i].data;
        }
    }

#ifdef NGX_DEBUG
    ngx_log_error_core(NGX_LOG_DEBUG, ngx_cycle->log, 0, "launch JVM");
#endif

    int ret = JNI_CreateJavaVM(&jmcf->jvm, (void **)&jmcf->jenv, &vm_args);
    if (ret < 0) {
        ngx_log_error_core(NGX_LOG_ERR, ngx_cycle->log, 0, "Unable to Launch JVM");
        return NGX_CONF_ERROR;
    }

    ngx_http_java_context_init(jmcf->jenv);
    ngx_http_java_servlet_context_init(jmcf->jenv);

    return NGX_CONF_OK;
}


static void *
ngx_http_java_create_main_conf(ngx_conf_t *cf)
{
    ngx_http_java_main_conf_t  *conf;

    conf = ngx_palloc(cf->pool, sizeof(ngx_http_java_main_conf_t));
    if (conf == NULL) {
        return NULL;
    }

    conf->vm_options = NGX_CONF_UNSET_PTR;

    return conf;
}

static void *
ngx_http_java_create_loc_conf(ngx_conf_t *cf)
{
    ngx_http_java_loc_conf_t  *conf;

    conf = ngx_pcalloc(cf->pool, sizeof(ngx_http_java_loc_conf_t));
    if (conf == NULL) {
        return NULL;
    }

    return conf;
}


static char *
ngx_http_java_merge_loc_conf(ngx_conf_t *cf, void *parent, void *child)
{
    ngx_http_java_loc_conf_t *prev = parent;
    ngx_http_java_loc_conf_t *conf = child;

    ngx_conf_merge_str_value(conf->dispatcher_name,
        prev->dispatcher_name, "org/nginx/Dispatcher");

    ngx_conf_merge_str_value(conf->handler_name,
        prev->handler_name, "");

    return NGX_CONF_OK;
}


static ngx_int_t
ngx_http_java_postconfiguration(ngx_conf_t *cf)
{

    return NGX_OK;
}


static ngx_int_t
ngx_http_java_init_process(ngx_cycle_t *cycle)
{
#ifdef NGX_DEBUG
    ngx_log_error_core(NGX_LOG_DEBUG, cycle->log, 0, "java module init");
#endif
    return NGX_OK;
}


static void
ngx_http_java_exit_process(ngx_cycle_t *cycle)
{
#ifdef NGX_DEBUG
    ngx_log_error_core(NGX_LOG_DEBUG, cycle->log, 0, "java module exit");
#endif
/*
    ngx_http_java_xslt_cleanup();

    xmlCleanupParser();
*/
}
