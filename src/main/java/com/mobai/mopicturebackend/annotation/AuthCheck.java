package com.mobai.mopicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义权限校验注解
 * <p>
 * 【知识点】Java元注解（Meta-Annotation）
 * Java提供了4种元注解来修饰其他注解：
 * 1. @Target - 指定注解可以用在哪些地方（类、方法、字段等）
 * 2. @Retention - 指定注解的生命周期（源码级、编译级、运行级）
 * 3. @Documented - 指定注解会被javadoc记录
 * 4. @Inherited - 指定注解可以被子类继承
 */

/**
 * @Target 元注解 - 指定注解的使用位置
 *
 * 【知识点】ElementType枚举常用值：
 * - ElementType.TYPE: 可以标注在类、接口、枚举上
 * - ElementType.FIELD: 可以标注在字段、枚举常量上
 * - ElementType.METHOD: 可以标注在方法上 ← 我们使用这个
 * - ElementType.PARAMETER: 可以标注在方法参数上
 * - ElementType.CONSTRUCTOR: 可以标注在构造器上
 * - ElementType.LOCAL_VARIABLE: 可以标注在局部变量上
 * - ElementType.ANNOTATION_TYPE: 可以标注在其他注解上
 *
 * 【应用场景】
 * AuthCheck注解只能用在方法上，例如：
 * @AuthCheck(mustRole = "admin")
 * public void deleteUser() { ... }
 */
@Target(ElementType.METHOD)

/**
 * @Retention 元注解 - 指定注解的保留策略（生命周期）
 *
 * 【知识点】RetentionPolicy枚举有3个值：
 * 1. RetentionPolicy.SOURCE - 源码级
 *    - 只在源代码中存在
 *    - 编译成.class文件后被丢弃
 *    - 例如：@Override, @SuppressWarnings
 *
 * 2. RetentionPolicy.CLASS - 编译级（默认值）
 *    - 在.class文件中存在
 *    - JVM运行时无法获取
 *    - 常用于字节码处理工具
 *
 * 3. RetentionPolicy.RUNTIME - 运行级 ← 我们使用这个
 *    - 在.class文件中存在
 *    - JVM运行时可以通过反射获取 ← 关键！
 *    - 配合反射可以实现AOP、权限校验等功能
 *
 * 【为什么选RUNTIME？】
 * 因为我们需要在程序运行时，通过AOP切面拦截带有@AuthCheck注解的方法，
 * 检查用户是否有权限执行该方法。如果注解在运行时不存在，就无法实现这个功能。
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须有该角色
     *
     * @return
     */
    String mustRole() default "";
}
