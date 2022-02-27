package com.petterp.router.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * 路由控制器,使用此控制器进行路由跳转
 * @author petterp
 */
object RouterSingleControl {

    private const val TAG = "RouterSingleControlTAG"

    // 存储所有隐射表信息
    private val mapping: MutableMap<String, String> = mutableMapOf()

    // 编译器生成的总映射表
    private const val GENERATED_MAPPING = "com.petterp.router.mapping.generated.RouterMapping"

    fun init() {
        try {
            // 反射去获取我们创建好的Mapping,将其缓存到控制器中
            val clazz = Class.forName(GENERATED_MAPPING)
            (clazz.getMethod("getMapping").invoke(null) as Map<String, String>).takeIf {
                it.isNotEmpty()
            }?.let { it ->
                Log.i(TAG, "init: getMapping->")
                mapping.putAll(it)
                mapping.forEach {
                    Log.i(TAG, "mapping-[key,value]  -> key:[${it.key}],value:[${it.value}]")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "init: error while init router : $e")
        }
    }

    /** 用于进行跳转 */
    fun go(context: Context, url: String) {
        if (url.isEmpty()) return
        // 用户的url可能还附带了其他值,比如 "router://home?name=xx"
        // 匹配URL,找到目标页面
        val uri = Uri.parse(url)
        // 例：scheme:router
        val scheme = uri.scheme
        // 例：host:home
        val host = uri.host
        // 例：path: " "
        val path = uri.path
        // 匹配到的class
        var targetActivityClass = ""
        mapping.forEach {
            val itemUri = Uri.parse(it.key)
            // router
            val itemScheme = itemUri.scheme
            // home
            val itemHost = itemUri.host
            // /profile
            val itemPath = itemUri.path
            if (itemScheme == scheme && itemHost == host && itemPath == path) {
                targetActivityClass = it.value
            }
        }
        if (targetActivityClass.isEmpty()) {
            Log.e(TAG, "go: no destination found")
            return
        }
        // 如下例所示: router://home/profile?name=xx
        // 解析URL里的参数，封装成为一个 Bundle
        val bundle = Bundle()
        // query长度最小为3
        uri.query?.takeIf {
            it.length >= 3
        }?.let { it ->
            val args = it.split("&")
            args.forEach {
                val splits = it.split("=")
                if (splits.size >= 2)
                    bundle.putString(splits[0], splits[1])
            }
        }

        // 打开对应的Activity,并传入Bundle
        try {
            val activity = Class.forName(targetActivityClass)
            context.startActivity(
                Intent(context, activity).apply {
                    this.putExtras(bundle)
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "go: startActivity: $targetActivityClass,error: $e ")
        }
    }
}
