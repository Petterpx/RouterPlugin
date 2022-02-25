package com.petterp.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.plugins.AppPlugin
import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import java.io.File

/**
 * 路由插件
 * 主要功能如下：
 * 1. 自动帮助用户把路径参数传递到注解处理器中
 * 2. 实现旧的构建产物自动清理
 * 3. 在javac任务后，汇总生成文档
 * @author petterp
 */
class RouterPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        // 1. 添加自定义的键值对
        project.extensions.create(ROUTER_EXTENSION, RouterExtension::class.java)
        project.task(testTaskName)

        // 2. 注册自定义的Transform
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            val appExtension = project.extensions.getByType(AppExtension::class.java)
            val transForm = RouterMappingTransForm()
            appExtension.registerTransform(transForm)
        }

        // 2. 为注解处理器添加参数,避免手动去build中kapt传递
        // 这里本来想让用户自己传递路径,但不同model都能配置键值对
        // 虽然可以根据app-model优先被调用这一特点，优先获取传递的参数，进行保留，后续model复用
        // 但是这个做法不够优雅，所以暂时就放弃,改用写死,后续再看看有没有别的方式
        val docPath = "${project.rootProject.projectDir.absolutePath}/router_doc"
        val file = File(docPath)
        if (!file.exists()) file.mkdirs()
        (project.extensions.findByName("kapt") as KaptExtension).apply {
            arguments {
                // 这里我们选择硬编码为项目目录
                arg("root_project_dir", docPath)
            }
        }

        if (!project.plugins.hasPlugin(AppPlugin::class.java)) return
        // 开始生成文档,在项目配置结束后
        project.afterEvaluate {
            val routerExtension = project.properties[ROUTER_EXTENSION] as RouterExtension
            if (routerExtension.wikiDir.isEmpty()) throw RuntimeException("尚未设置文档路径")
            println("-用户设置的wiki路径------${routerExtension.wikiDir}")
            // 在javac任务(compileDebugJavaWithJavac)后，汇总生成的文档
            project.tasks.asSequence().filter { task ->
                task.name.startsWith("compile") && task.name.endsWith("JavaWithJavac") && task != null
            }.forEach {
                it.doLast {
                    // 读一下我们生成的json,开始进行汇总
                    val routerMappingDir =
                        File(docPath, "router_mapping")
                    if (!routerMappingDir.exists()) return@doLast
                    val allChildFiles = routerMappingDir.listFiles()
                    if (allChildFiles.isNullOrEmpty()) return@doLast

                    val mdBuilder = StringBuilder("# 页面文档\n\n")
                    allChildFiles.filter { childFile ->
                        childFile.name.endsWith(".json")
                    }.forEach { file ->
                        val jsonSlurper = JsonSlurper()
                        (jsonSlurper.parse(file) as? ArrayList<Map<String, String>>)?.forEach { map ->
                            val url = map["url"]
                            val description = map["description"]
                            val realPath = map["realPath"]
                            mdBuilder.append("\n## $description\n")
                            mdBuilder.append("- url=$url\n")
                            mdBuilder.append("- realPath=$realPath\n")
                        }
                    }

                    // 通过用户传入的wiki路径生成
                    val wikiFileDir = File(routerExtension.wikiDir)
                    if (!wikiFileDir.exists()) wikiFileDir.mkdir()
                    val wikiFile = File(wikiFileDir, "页面文档.md")
                    if (wikiFile.exists()) wikiFile.delete()
                    wikiFile.writeText(mdBuilder.toString())
                }
            }
        }
    }

    companion object {
        const val ROUTER_EXTENSION = "router"
        const val testTaskName = "routerTask"
    }
}
