package com.petterp.router.processor

import com.google.auto.service.AutoService
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.petterp.router.annotations.Router
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

/**
 * 路由组件的注解处理器,用于找到我们在相应的类上标注的 @[com.petterp.router.annotations.Router] 注解
 * 1. 生成相应的路由调用类
 * 2. 生成相应的路由json
 * @author petterp
 */
@AutoService(Processor::class)
class RouterProcessor : AbstractProcessor() {

    /** 编译器找到我们的注解后会回调此方法
     * @param set 找到的注解
     * */
    override fun process(
        set: MutableSet<out TypeElement>,
        roundEnvironment: RoundEnvironment
    ): Boolean {
        // 避免多次调用process
        if (roundEnvironment.processingOver()) return false
        println("$TAG-------start")

        // 获得相应路由mapping管理类与json内容
        val (mapping, jsonArray) = mappingManagerToJsonContent(roundEnvironment) ?: return false
        // 1. 生成相应的mapping类
        writeMappingClassManager(mapping)
        // 2. 生成路由json
        // ps: root_project_dir是我们给注解处理器传递的参数,具体见 [RouterPlugin]
        val rootDir = processingEnv.options["root_project_dir"]
        writeMapping(rootDir, jsonArray)

        println("$TAG-------finish")
        return false
    }

    /** 告诉编译器当前处理器支持的注解类型 */
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Router::class.java.name)
    }

    private fun mappingManagerToJsonContent(roundEnvironment: RoundEnvironment): Pair<Map<String, String>, JsonArray>? {
        // 获取所有标记了@Router注解类的信息
        val elementsAnnotatedWith = roundEnvironment.getElementsAnnotatedWith(Router::class.java)
        println("$TAG------收集到了${elementsAnnotatedWith.size}个使用Router的类信息")
        // 当未收集到@Router注解时，跳过
        if (elementsAnnotatedWith.size < 1) return null
        val jsonArray = JsonArray()
        val mapping = mutableMapOf<String, String>()
        elementsAnnotatedWith.forEach {
            (it as? TypeElement)?.let { element ->
                // 获取注解信息
                element.getAnnotation(Router::class.java)?.apply {
                    // 获取注解类的全类名
                    val realPath = element.qualifiedName.toString()
                    // 为下面的mapping表做准备
                    mapping[url] = realPath
                    // 保存路由信息json
                    jsonArray.add(getRouterJsonObject(url, description, realPath))
                    println("$TAG--->url:$url---->description:$description---->className:$realPath")
                }
            }
        }
        return mapping to jsonArray
    }

    private fun writeMappingClassManager(mapping: Map<String, String>) {
        val className = "RouterMapping_" + System.currentTimeMillis()
        val writeMappingClassContent = getMappingClassContent(className, mapping)
        val mappingFullClassName = "com.petterp.router.mapping.$className"
        println("$TAG-----> mappingFullClassName = $mappingFullClassName")
        println("$TAG----->class content=\n$writeMappingClassContent")
        val source = processingEnv.filer.createSourceFile(mappingFullClassName)
        source.openWriter().use {
            it.write(writeMappingClassContent)
        }
    }

    private fun writeMapping(rootDir: String?, jsonArray: JsonArray) {
        if (rootDir == null) throw RuntimeException("$rootDir non null!")
        val rootFile = File(rootDir)
        if (!rootFile.exists()) throw RuntimeException("$rootDir not exists!")

        val routerFile = File(rootFile, "router_mapping")
        if (!routerFile.exists()) routerFile.mkdir()
        // 这里选择移除router_mapping下的的json,我们希望它只保留最新的一份
//        else routerFile.listFiles()?.forEach {
//            it.delete()
//        }
        val mappingFile = File(routerFile, "mapping_${System.currentTimeMillis()}.json")
        mappingFile.bufferedWriter().use {
            it.write(jsonArray.toString())
        }
    }

    private fun getRouterJsonObject(url: String, description: String, realPath: String) =
        JsonObject().apply {
            addProperty("url", url)
            addProperty("description", description)
            addProperty("realPath", realPath)
        }

    private fun getMappingClassContent(className: String, map: Map<String, String>): String {
        // 将要自动生成的类的类名
        val builder = StringBuilder()
        builder.append("package com.petterp.router.mapping;\n")
        builder.append("import java.util.HashMap;\n")
        builder.append("import java.util.Map;\n")
        builder.append(
            "\n/**\n" +
                " * 自动生成的路由表\n" +
                " * @author petterp\n" +
                " */\n"
        )
            .append("public class ").append(className).append(" {\n")
            .append("    private static final HashMap<String, String> mapping = new HashMap<>();\n")
            .append(
                "    public static Map<String, String> getMapping() {\n" +
                    "        return mapping;\n" +
                    "    }\n"
            ).append(
                "\n    protected static void addKeyValue(String key, String value) {\n" +
                    "        mapping.put(key, value);\n" +
                    "    }"
            )
        if (map.isNotEmpty()) {
            builder.append("\n    static {\n")
            map.forEach { (url, realPath) ->
                builder.append("      addKeyValue(\"$url\",\"$realPath\");\n")
            }
            builder.append("    }\n")
        }
        builder.append("}\n")
        return builder.toString()
    }

    companion object {
        private const val TAG = "RouterProcessor"
    }
}
