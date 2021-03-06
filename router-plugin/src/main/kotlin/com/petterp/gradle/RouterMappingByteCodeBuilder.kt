package com.petterp.gradle

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*

/**
 * 路由字节码创建者
 * 如何查看路由类是否创建完成：
 * 查看build/intermediates/transforms/RouterMappingTransForm/xxx/xx.jar
 * 具体是哪个jar包看打印的日志信息
 * @author petterp
 */
class RouterMappingByteCodeBuilder : Opcodes {
    companion object {
        const val CLASS_NAME = "com/petterp/router/mapping/generated/RouterMapping"
        fun get(allMappingNames: Set<String>): ByteArray {
            // 1. 创建一个类
            // 2. 创建静态mapping持有者
            // 2. 创建构造方法
            // 3. 创建get方法
            //      2. 塞入所有隐射表内容
            //      3. 返回map
            return ClassWriter(ClassWriter.COMPUTE_MAXS).apply {
                visit(
                    V1_8, ACC_PUBLIC + ACC_SUPER, CLASS_NAME,
                    null, "java/lang/Object", null
                )
                createStateFieldMapping(this)
                createConstructor(this)
                createGetMapping(this)
                createAddKeyValue(this)
                createStaticInit(this, allMappingNames)
                visitEnd()
            }.toByteArray()
        }

        private fun createStateFieldMapping(cw: ClassWriter) {
            val fieldVisitor = cw.visitField(
                ACC_PRIVATE or ACC_FINAL or ACC_STATIC,
                "mapping",
                "Ljava/util/HashMap;",
                "Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/String;>;",
                null
            )
            fieldVisitor.visitEnd()
        }

        private fun createConstructor(cw: ClassWriter) {
            val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
            // 开启字节码生成或访问
            mv.visitCode()
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            // 调用某个类的方法
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            mv.visitInsn(RETURN)
            // 设置局部变量的栈帧大小
            mv.visitMaxs(1, 1)
            mv.visitEnd()
        }

        private fun createGetMapping(cw: ClassWriter) {
            // 创建getMapping方法
            val mv = cw.visitMethod(
                ACC_PUBLIC or ACC_STATIC,
                "getMapping",
                "()Ljava/util/Map;",
                "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                null
            )
            mv.visitCode()
            mv.visitFieldInsn(
                GETSTATIC,
                CLASS_NAME,
                "mapping",
                "Ljava/util/HashMap;"
            )
            mv.visitInsn(ARETURN)
            mv.visitMaxs(1, 0)
            mv.visitEnd()
        }

        private fun createAddKeyValue(cw: ClassWriter) {
            val mv = cw.visitMethod(
                ACC_PROTECTED or ACC_STATIC,
                "addKeyValue",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                null,
                null
            )
            mv.visitCode()
            val label0 = Label()
            mv.visitLabel(label0)
            mv.visitFieldInsn(
                GETSTATIC,
                CLASS_NAME,
                "mapping",
                "Ljava/util/HashMap;"
            )
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/util/HashMap",
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                false
            )
            mv.visitInsn(POP)
            mv.visitInsn(RETURN)
            val label2 = Label()
            mv.visitLabel(label2)
            mv.visitLocalVariable("key", "Ljava/lang/String;", null, label0, label2, 0)
            mv.visitLocalVariable("value", "Ljava/lang/String;", null, label0, label2, 1)
            mv.visitMaxs(3, 2)
            mv.visitEnd()
        }

        private fun createStaticInit(
            cw: ClassWriter,
            allMappingNames: Set<String>
        ) {
            val mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null)
            mv.visitCode()
            mv.visitTypeInsn(NEW, "java/util/HashMap")
            mv.visitInsn(DUP)
            mv.visitMethodInsn(
                INVOKESPECIAL,
                "java/util/HashMap",
                "<init>",
                "()V",
                false
            )
            mv.visitFieldInsn(
                PUTSTATIC,
                CLASS_NAME,
                "mapping",
                "Ljava/util/HashMap;"
            )

            // 开始写入
            allMappingNames.forEach {
                val classPath = "com/petterp/router/mapping/$it"
                mv.visitFieldInsn(
                    GETSTATIC,
                    CLASS_NAME,
                    "mapping",
                    "Ljava/util/HashMap;"
                )
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    classPath,
                    "getMapping",
                    "()Ljava/util/Map;",
                    false
                )
                mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/util/HashMap",
                    "putAll",
                    "(Ljava/util/Map;)V",
                    false
                )
            }
            mv.visitInsn(RETURN)
            mv.visitMaxs(2, 2)
            mv.visitEnd()
        }
    }
}
