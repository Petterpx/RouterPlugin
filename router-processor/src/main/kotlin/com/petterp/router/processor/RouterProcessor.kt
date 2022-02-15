package com.petterp.router.processor

import com.google.auto.service.AutoService
import com.petterp.router.annotations.Router
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

/**
 *
 * @author petterp
 */
@AutoService(Processor::class)
class RouterProcessor : AbstractProcessor() {

    companion object {
        private const val TAG = "RouterProcessor"
    }

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
        // 获取所有标记了@Router注解类的信息
        val elementsAnnotatedWith = roundEnvironment.getElementsAnnotatedWith(Router::class.java)
        println("$TAG----------count----${elementsAnnotatedWith.size}")
        // 当未收集到@Router注解时，跳过
        if (elementsAnnotatedWith.size < 1) return false
        val mapping = mutableMapOf<String, String>()
        elementsAnnotatedWith.forEach {
            (it as? TypeElement)?.let { element ->
                // 获取注解信息
                element.getAnnotation(Router::class.java)?.apply {
                    // 获取注解类的全类名
                    val realPath = element.qualifiedName.toString()
                    mapping[url] = realPath
                    println("$TAG--->url:$url---->description:$description---->className:$realPath")
                }
            }
        }
        val className = "RouterMapping_" + System.currentTimeMillis()
        val writeMappingClassContent = writeMapping(className, mapping)
        val mappingFullClassName = "com.petterp.router.mapping.$className"
        println("$TAG-----> mappingFullClassName = $mappingFullClassName")
        println("$TAG----->class content=\n$writeMappingClassContent")
        try {
            val source = processingEnv.filer.createSourceFile(mappingFullClassName)
            val openWriter = source.openWriter()
            openWriter.write(writeMappingClassContent)
            openWriter.flush()
            openWriter.close()
        } catch (e: Exception) {
            throw RuntimeException("Error write create file", e)
        }
        println("$TAG-------finish")
        return false
    }

    /** 告诉编译器当前处理器支持的注解类型 */
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Router::class.java.name)
    }

    private fun writeMapping(className: String, map: Map<String, String>): String {
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
}
