package com.petterp.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 * @author petterp
 */
class RouterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(ROUTER_EXTENSION, RouterExtension::class.java)
        // 当前project配置结束
        project.task(testTaskName)
        // 当前工程配置阶段已经结束
        project.afterEvaluate {
            val routerExtension = project.properties[ROUTER_EXTENSION] as RouterExtension
            println("-用户设置的wiki路径------${routerExtension.wikiDir}")
        }
    }

    companion object {
        const val ROUTER_EXTENSION = "router"
        const val testTaskName = "routerTask"
    }
}
