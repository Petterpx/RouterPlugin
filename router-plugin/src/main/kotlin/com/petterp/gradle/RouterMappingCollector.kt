package com.petterp.gradle

import java.io.File
import java.util.jar.JarFile

/**
 * 路由映射信息收集者,主要收集生成的相应的Mapping类
 * @author petterp
 */
class RouterMappingCollector {

    private val _mappingClassNames = mutableSetOf<String>()
    val mappingClassNames: Set<String> = _mappingClassNames

    /**
     * 收集class文件或者class文件目录中的映射表类
     * */
    fun collect(classFile: File?) {
        if (classFile?.exists() != true) return
        if (classFile.isFile) {
            if (classFile.absolutePath.contains(PACKAGE_NAME) &&
                classFile.name.startsWith(
                        CLASS_NAME_PREFIX
                    ) && classFile.name.endsWith(CLASS_FILE_SUFFIX)
            ) {
                val className = classFile.name.replace(CLASS_FILE_SUFFIX, "")
                _mappingClassNames.add(className)
            }
        } else {
            classFile.listFiles()?.forEach {
                collect(it)
            }
        }
    }

    /** 收集Jar包中的目标类 */
    fun collectFromJarFile(jarFile: File) {
        val enumeration = JarFile(jarFile).entries()
        enumeration.asIterator().forEach {
            val name = it.name
            if (name.contains(PACKAGE_NAME) && name.contains(CLASS_NAME_PREFIX) && name.contains(
                    CLASS_FILE_SUFFIX
                )
            ) {
                val className = name.replace(PACKAGE_NAME, "")
                    .replace("/", "")
                    .replace(CLASS_FILE_SUFFIX, "")
                _mappingClassNames.add(className)
            }
        }
    }

    companion object {
        const val PACKAGE_NAME = "com/petterp/router/mapping"
        const val CLASS_NAME_PREFIX = "RouterMapping_"
        const val CLASS_FILE_SUFFIX = ".class"
    }
}
