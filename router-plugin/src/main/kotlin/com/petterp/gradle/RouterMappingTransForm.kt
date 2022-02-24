package com.petterp.gradle

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 *
 * @author petterp
 */
class RouterMappingTransForm : Transform() {
    override fun getName(): String = "RouterMappingTransForm"

    /**
     * 返回告知编译器，当前Transform需要消费的输入类型
     * 在这里是class类型
     * */
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 告诉编译器，Transform的作用范围
     * */
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 告诉编译器,Transform是否支持增量
     * */
    override fun isIncremental(): Boolean {
        return false
    }

    /**
     * 所有class收集好以后,会被打包到此方法进行处理
     * */
    override fun transform(transformInvocation: TransformInvocation) {
        // 1. 遍历所有的Input
        // 2. 对Input进行二次处理
        // 3. 将Input拷贝到目标目录

        val collector = RouterMappingCollector()

        // 遍历所有的输入
        transformInvocation.inputs.forEach {
            // 把 文件夹 类型的输入,拷贝到目标目录
            it.directoryInputs.forEach { direInput ->
                val destDir = transformInvocation.outputProvider.getContentLocation(
                    direInput.name,
                    direInput.contentTypes,
                    direInput.scopes,
                    Format.DIRECTORY
                )
                collector.collect(direInput.file)
                direInput.file.copyRecursively(destDir)
            }

            // 把 jar 类型的输入,拷贝到目标目录
            it.jarInputs.forEach { jarInput ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jarInput.name,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )
                collector.collectFromJarFile(jarInput.file)
                jarInput.file.copyRecursively(dest)
            }
        }
        println("$name----all mapping class name = ${collector.mappingClassNames}")

        val mappingJarFile = transformInvocation.outputProvider.getContentLocation(
            "router_mapping",
            outputTypes, scopes, Format.JAR
        )
        println("$name   mappingJarFile = $mappingJarFile")
        if (!mappingJarFile.parentFile.exists()) mappingJarFile.parentFile.mkdirs()
        if (mappingJarFile.exists()) mappingJarFile.delete()

        // 将生成的字节码写入本地文件
        val zipEntry = ZipEntry(RouterMappingByteCodeBuilder.CLASS_NAME + ".class")
        val fos = mappingJarFile.outputStream()
        val jarOutputStream = JarOutputStream(fos)
        jarOutputStream.putNextEntry(zipEntry)
        val byteArray = RouterMappingByteCodeBuilder.get(collector.mappingClassNames)
        jarOutputStream.write(byteArray)
        jarOutputStream.closeEntry()
        jarOutputStream.close()
        fos.close()
    }
}
