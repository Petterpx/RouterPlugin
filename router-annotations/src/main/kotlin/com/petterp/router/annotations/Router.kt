package com.petterp.router.annotations

/**
 * 路由注解标识
 * @author petterp
 */
// 说明当前注解可以修饰的元素，此处标识可以用于标记在类上面
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
// 注解可以被保留的时机
// source 编译期不保留
// class 编译期保留
// runtime 运行时可获取
@Retention(AnnotationRetention.BINARY)
annotation class Router(
    /** 当前页面的Url */
    val url: String,
    /** 页面描述 */
    val description: String
)
