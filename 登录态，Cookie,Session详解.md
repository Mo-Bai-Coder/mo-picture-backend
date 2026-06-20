# Java Web 登录态、Session、Cookie 详解

> 📚 适合小白学习的完整指南，包含概念讲解、代码示例和实际应用

---

## 📖 目录

1. [HTTP 协议的无状态特性](#1-http-协议的无状态特性)
2. [为什么需要会话管理](#2-为什么需要会话管理)
3. [Session 和 Cookie 核心概念](#3-session-和-cookie-核心概念)
4. [完整的登录流程](#4-完整的登录流程)
5. [Java 注解元知识](#5-java-注解元知识)
6. [AOP 权限拦截实战](#6-aop-权限拦截实战)
7. [常见问题解答](#7-常见问题解答)

---

## 1. HTTP 协议的无状态特性

### 1.1 什么是"无状态"？

**HTTP 是无状态协议**，意思是：
- 每次请求都是独立的
- 服务器不会记住之前的请求
- 服务器不知道两次请求是否来自同一个用户

### 1.2 生活中的比喻

```
你去银行办理业务：
❌ 无状态：每次去窗口，柜员都不认识你，需要你重新出示身份证
✅ 有状态：柜员认识你，知道你是老客户，直接办理业务

HTTP 就是 ❌ 无状态的情况
```

### 1.3 带来的问题

如果没有会话管理，会出现以下问题：

```
用户操作流程：
1. 访问首页 → 服务器：你好，陌生人
2. 点击登录 → 服务器：好的，已验证你是 admin
3. 访问个人中心 → 服务器：你是谁？我不认识你 😅
4. 再次访问个人中心 → 服务器：你到底是谁？😅
```

**结论**：每次请求都需要重新登录，用户体验极差！

---

## 2. 为什么需要会话管理

### 2.1 解决方案：Session + Cookie

为了解决 HTTP 无状态的问题，引入了**会话管理机制**：

```
┌─────────────────────────────────────────────┐
│           会话管理的核心思想                  │
├─────────────────────────────────────────────┤
│                                             │
│  服务器给每个用户发一个"身份证"（sessionId）  │
│  用户每次请求都带上这个"身份证"               │
│  服务器通过"身份证"识别用户身份               │
│                                             │
└─────────────────────────────────────────────┘
```

### 2.2 两个关键角色

| 角色 | 存储位置 | 作用 | 比喻 |
|------|---------|------|------|
| **Cookie** | 浏览器（客户端） | 存储 sessionId | 会员卡号 |
| **Session** | 服务器 | 存储用户完整信息 | 会员档案 |

---

## 3. Session 和 Cookie 核心概念

### 3.1 Cookie 详解

#### 什么是 Cookie？

Cookie 是服务器发送到浏览器的一小段数据，浏览器会保存它，并在后续请求中自动发送回服务器。

#### Cookie 的特点

```
📦 存储位置：浏览器（客户端）
🔒 安全性：较低（用户可以查看和修改）
📏 容量限制：约 4KB
⏰ 生命周期：可设置过期时间，默认关闭浏览器后失效
📝 内容格式：键值对，如 JSESSIONID=abc123xyz
```

#### Cookie 的工作原理

```java
// 服务器设置 Cookie（响应头）
Set-Cookie: JSESSIONID=abc123xyz; Path=/; HttpOnly

// 浏览器自动携带 Cookie（请求头）
Cookie: JSESSIONID=abc123xyz
```

### 3.2 Session 详解

#### 什么是 Session？

Session 是服务器端创建的会话对象，用于存储用户的状态信息。

#### Session 的特点

```
📦 存储位置：服务器内存（或 Redis 等）
🔒 安全性：较高（用户无法直接访问）
📏 容量限制：较大（取决于服务器内存）
⏰ 生命周期：默认 30 分钟无操作后过期
📝 内容格式：可以存储任意对象
```

#### Session 的结构

```
Session 对象 {
    sessionId: "abc123xyz",          // 唯一标识
    createTime: 2024-01-01 10:00:00, // 创建时间
    lastAccessTime: 2024-01-01 10:30:00, // 最后访问时间
    
    // 存储的用户数据
    attributes: {
        "user": PictureUser对象,
        "userId": 123,
        "userName": "admin",
        "userRole": "admin"
    }
}
```

### 3.3 Cookie vs Session 对比表

```
┌──────────┬──────────────────┬──────────────────┐
│   特性    │     Cookie       │     Session      │
├──────────┼──────────────────┼──────────────────┤
│ 存储位置  │ 浏览器（客户端）   │ 服务器            │
│ 安全性    │ 较低（可被篡改）   │ 较高              │
│ 容量      │ 小（4KB左右）     │ 大                │
│ 性能      │ 不占用服务器资源   │ 占用服务器内存     │
│ 作用      │ 存储 sessionId    │ 存储用户完整信息   │
│ 生命周期  │ 可设置过期时间     │ 默认30分钟超时     │
│ 跨域      │ 受同源策略限制     │ 不受限制          │
└──────────┴──────────────────┴──────────────────┘
```

### 3.4 通俗比喻：商场会员系统

```
🏪 场景：你去商场购物

1️⃣ 首次办卡（登录）
   - 你在服务台填写信息（用户名、密码）
   - 服务台创建会员档案（Session），存入数据库
   - 给你一张会员卡，上面有卡号（Cookie，含 sessionId）

2️⃣ 后续购物（访问接口）
   - 你出示会员卡（浏览器自动发送 Cookie）
   - 服务员根据卡号查询会员档案（根据 sessionId 查找 Session）
   - 知道你是谁、有什么等级、享受什么折扣（获取用户信息）

3️⃣ 卡丢了（Cookie 失效）
   - 需要重新办卡（重新登录）

4️⃣ 档案被销毁（Session 过期）
   - 即使有卡号，也查不到信息（需要重新登录）
```

---

## 4. 完整的登录流程

### 4.1 流程图

```
┌─────────────────────────────────────────────────────────────┐
│                    完整的登录流程                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  第1步：用户登录                                             │
│  ┌────────┐         POST /login          ┌────────┐        │
│  │ 浏览器  │ ──────────────────────────→ │ 服务器  │        │
│  │        │                              │        │        │
│  │        │  {username:"admin",          │        │        │
│  │        │   password:"123456"}         │        │        │
│  └────────┘                              └────────┘        │
│                                                              │
│  第2步：服务器验证并创建 Session                             │
│  ┌────────┐                                                 │
│  │ 服务器  │  1. 验证用户名密码                              │
│  │        │  2. 创建 Session 对象                           │
│  │        │     sessionId = "abc123xyz"                     │
│  │        │     session.setAttribute("user", userObj)       │
│  │        │  3. 将 Session 存储在服务器内存中                │
│  └────────┘                                                 │
│                                                              │
│  第3步：服务器响应，设置 Cookie                              │
│  ┌────────┐         响应头                 ┌────────┐        │
│  │ 服务器  │ ──────────────────────────→ │ 浏览器  │        │
│  │        │  Set-Cookie:                  │        │        │
│  │        │    JSESSIONID=abc123xyz       │        │        │
│  └────────┘                              └────────┘        │
│                                                              │
│  第4步：浏览器保存 Cookie                                    │
│  ┌────────┐                                                 │
│  │ 浏览器  │  自动保存 Cookie 到本地                         │
│  │        │  Cookie: JSESSIONID=abc123xyz                   │
│  └────────┘                                                 │
│                                                              │
│  第5步：后续请求自动携带 Cookie                              │
│  ┌────────┐         GET /user/info        ┌────────┐        │
│  │ 浏览器  │ ──────────────────────────→ │ 服务器  │        │
│  │        │  Cookie:                      │        │        │
│  │        │    JSESSIONID=abc123xyz       │        │        │
│  └────────┘                              └────────┘        │
│                                                              │
│  第6步：服务器根据 Cookie 获取用户信息                       │
│  ┌────────┐                                                 │
│  │ 服务器  │  1. 从 Cookie 取出 sessionId = "abc123xyz"     │
│  │        │  2. 根据 sessionId 查找 Session                 │
│  │        │  3. 从 Session 获取 user 对象                   │
│  │        │  4. 返回用户信息                                │
│  └────────┘                                                 │
│                                                              │
│  ✅ 这就是"登录态"！                                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 代码实现示例

#### 登录接口（Controller）

```java
@PostMapping("/login")
public Result<LoginUserVO> login(@RequestBody UserLoginRequest request) {
    // 1. 参数校验
    String username = request.getUsername();
    String password = request.getPassword();
    
    // 2. 查询数据库，验证用户名密码
    PictureUser user = userService.getUserByUsername(username);
    if (user == null || !password.equals(user.getPassword())) {
        return Result.error("用户名或密码错误");
    }
    
    // 3. 创建 Session，存储用户信息
    HttpSession session = httpRequest.getSession(true); // true表示不存在则创建
    session.setAttribute("user", user);
    
    // 4. 返回登录成功
    LoginUserVO loginUserVO = new LoginUserVO();
    loginUserVO.setId(user.getId());
    loginUserVO.setUsername(user.getUsername());
    return Result.success(loginUserVO);
}
```

#### 获取当前登录用户（Service）

```java
/**
 * 获取当前登录用户
 * 
 * @param request HTTP 请求对象
 * @return 登录用户对象
 */
public PictureUser getLoginUser(HttpServletRequest request) {
    // 1. 从 Cookie 中获取 sessionId（由 Tomcat 自动完成）
    // 2. 根据 sessionId 从 Session 中获取用户信息
    HttpSession session = request.getSession(false); // false表示不创建新session
    
    if (session == null) {
        throw new BusinessException("未登录");
    }
    
    Object userObj = session.getAttribute("user");
    
    if (userObj == null) {
        throw new BusinessException("未登录");
    }
    
    return (PictureUser) userObj;
}
```

#### 退出登录

```java
@PostMapping("/logout")
public Result logout(HttpServletRequest request) {
    // 销毁 Session
    HttpSession session = request.getSession(false);
    if (session != null) {
        session.invalidate(); // 销毁 session
    }
    
    return Result.success("退出成功");
}
```

---

## 5. Java 注解元知识

### 5.1 什么是注解（Annotation）？

注解是 Java 5 引入的特性，用于给代码添加元数据（描述数据的数据）。

```java
// 常见的内置注解
@Override        // 表示方法重写了父类方法
@Deprecated      // 表示方法已过时
@SuppressWarnings // 忽略编译器警告
```

### 5.2 元注解（Meta-Annotation）

元注解是用来修饰其他注解的注解。Java 提供了 4 种元注解：

| 元注解 | 作用 | 示例 |
|--------|------|------|
| `@Target` | 指定注解可以用在哪些地方 | 类、方法、字段等 |
| `@Retention` | 指定注解的生命周期 | 源码级、编译级、运行级 |
| `@Documented` | 指定注解会被 javadoc 记录 | - |
| `@Inherited` | 指定注解可以被子类继承 | - |

### 5.3 @Target 详解

#### ElementType 枚举常用值

```java
public enum ElementType {
    TYPE,           // 类、接口、枚举
    FIELD,          // 字段、枚举常量
    METHOD,         // 方法 ← 我们使用这个
    PARAMETER,      // 方法参数
    CONSTRUCTOR,    // 构造器
    LOCAL_VARIABLE, // 局部变量
    ANNOTATION_TYPE,// 其他注解
    PACKAGE         // 包
}
```

#### 应用场景

```java
// AuthCheck 注解只能用在方法上
@Target(ElementType.METHOD)
public @interface AuthCheck {
    String mustRole() default "";
}

// 使用示例
@AuthCheck(mustRole = "admin")
public void deleteUser() {
    // 删除用户逻辑
}
```

### 5.4 @Retention 详解

#### RetentionPolicy 枚举的 3 个值

```java
public enum RetentionPolicy {
    SOURCE,   // 源码级
    CLASS,    // 编译级（默认值）
    RUNTIME   // 运行级 ← 我们使用这个
}
```

#### 三种级别对比

```
1️⃣ RetentionPolicy.SOURCE（源码级）
   - 只在源代码中存在
   - 编译成 .class 文件后被丢弃
   - 例如：@Override, @SuppressWarnings
   - 用途：编译器检查

2️⃣ RetentionPolicy.CLASS（编译级）
   - 在 .class 文件中存在
   - JVM 运行时无法获取
   - 用途：字节码处理工具（如 ASM、ByteBuddy）

3️⃣ RetentionPolicy.RUNTIME（运行级）
   - 在 .class 文件中存在
   - JVM 运行时可以通过反射获取 ← 关键！
   - 用途：AOP、权限校验、依赖注入等
```

#### 为什么选择 RUNTIME？

```java
/**
 * 因为我们需要在程序运行时，通过 AOP 切面拦截带有 @AuthCheck 注解的方法，
 * 检查用户是否有权限执行该方法。如果注解在运行时不存在，就无法实现这个功能。
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    String mustRole() default "";
}
```

### 5.5 自定义注解完整示例

```java
package com.mobai.mopicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义权限校验注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    
    /**
     * 必须有该角色
     * @return 角色名称（如："admin"、"user"）
     */
    String mustRole() default "";
}
```

---

## 6. AOP 权限拦截实战

### 6.1 什么是 AOP？

**AOP（Aspect-Oriented Programming）**：面向切面编程，用于将横切关注点（如日志、事务、权限）从业务逻辑中分离出来。

#### 生活中的比喻

```
🏢 公司安保系统：

传统方式（OOP）：
- 每个办公室门口都安排一个保安
- 代码重复，维护困难

AOP 方式：
- 在大楼入口设置统一的安检门
- 所有进入的人都要经过安检
- 代码集中，易于维护
```

### 6.2 AOP 核心概念

| 概念 | 说明 | 示例 |
|------|------|------|
| **切面（Aspect）** | 横切关注点的模块化 | 权限校验模块 |
| **通知（Advice）** | 在特定连接点执行的动作 | 权限校验逻辑 |
| **连接点（JoinPoint）** | 程序执行的某个点 | 方法调用 |
| **切入点（Pointcut）** | 匹配连接点的表达式 | `@annotation(AuthCheck)` |
| **目标对象（Target）** | 被代理的对象 | Controller 方法 |

### 6.3 通知类型

```
┌────────────────────────────────────────┐
│           AOP 通知类型                  │
├──────────────┬─────────────────────────┤
│ @Before      │ 目标方法执行前           │
│ @After       │ 目标方法执行后（无论是否异常）│
│ @AfterReturning │ 目标方法成功返回后     │
│ @AfterThrowing  │ 目标方法抛出异常后     │
│ @Around      │ 环绕通知（前后都可以插入逻辑）│
└──────────────┴─────────────────────────┘
```

### 6.4 权限拦截器完整代码

```java
package com.mobai.mopicturebackend.aop;

import com.mobai.mopicturebackend.annotation.AuthCheck;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.model.entity.PictureUser;
import com.mobai.mopicturebackend.model.enums.UserRoleEnum;
import com.mobai.mopicturebackend.service.userService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验拦截器（AOP切面）
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private userService userService;

    /**
     * 权限校验拦截方法（环绕通知）
     * 
     * @Around 说明：
     * - 在目标方法执行前后都可以插入逻辑
     * - 可以决定是否执行目标方法（joinPoint.proceed()）
     * - "@annotation(authCheck)" 表示拦截所有带有 @AuthCheck 注解的方法
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {

        // 获取注解中要求的角色（如："admin"、"user"）
        String mustRole = authCheck.mustRole();
        
        /**
         * ========================================================================
         * 【重点讲解】如何从 HTTP 请求中获取当前登录用户？
         * ========================================================================
         * 
         * 🎯 目标：获取 HttpServletRequest 对象
         * 
         * ❓ 为什么需要 HttpServletRequest？
         * - 因为用户的登录信息（sessionId）存储在 Cookie 中
         * - Cookie 会随着每次请求自动发送到服务器
         * - HttpServletRequest 包含了请求的所有信息（请求头、Cookie、参数等）
         * 
         * 📝 获取 HttpServletRequest 的两种方式：
         * 
         * 方式1️⃣：作为方法参数直接注入（推荐用于 Controller）
         * ```java
         * @GetMapping("/info")
         * public Result getUserInfo(HttpServletRequest request) {
         *     // 直接使用 request
         * }
         * ```
         * 
         * 方式2️⃣：通过 RequestContextHolder 获取（用于非 Controller 层）← 我们用这个
         * - AOP 切面、Service 层等非 Controller 地方无法直接注入 request
         * - Spring 提供了 RequestContextHolder 工具类
         * - 它基于 ThreadLocal 机制，可以在同一线程的任何地方获取 request
         */
        
        /**
         * 步骤1：获取请求上下文属性
         * 
         * RequestContextHolder 是什么？
         * - Spring 提供的工具类，用于在当前线程中共享请求信息
         * - 底层使用 ThreadLocal 存储 RequestAttributes
         * - Spring MVC 在处理请求时，会自动将 request 存入 RequestContextHolder
         * 
         * ThreadLocal 小知识：
         * - 每个线程都有自己独立的存储空间
         * - 就像酒店每个房间都有自己的保险箱
         * - 线程A存的数据，线程B拿不到（隔离性好）
         * 
         * currentRequestAttributes() 返回什么？
         * - 返回 RequestAttributes 接口类型
         * - 实际对象是 ServletRequestAttributes（Web应用）
         * - 里面封装了 HttpServletRequest 和 HttpServletResponse
         */
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        
        /**
         * 步骤2：类型转换，获取 HttpServletRequest
         * 
         * 为什么要强制转换？
         * - currentRequestAttributes() 返回的是接口类型 RequestAttributes
         * - 我们需要的是具体实现类 ServletRequestAttributes
         * - ServletRequestAttributes 才有 getRequest() 方法
         * 
         * 转换后能拿到什么？
         * - HttpServletRequest 对象
         * - 包含了本次请求的所有信息：
         *   • 请求头（Headers）
         *   • Cookie（包含 JSESSIONID）
         *   • 请求参数（Parameters）
         *   • 请求方法（GET/POST等）
         *   • 请求路径（URL）
         */
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        
        /**
         * ========================================================================
         * 【关键步骤】获取当前登录用户
         * ========================================================================
         * 
         * getLoginUser(request) 内部做了什么？
         * 
         * 伪代码示例：
         * ```java
         * public PictureUser getLoginUser(HttpServletRequest request) {
         *     // 1. 从 Cookie 中获取 sessionId
         *     Cookie[] cookies = request.getCookies();
         *     String sessionId = null;
         *     for (Cookie cookie : cookies) {
         *         if ("JSESSIONID".equals(cookie.getName())) {
         *             sessionId = cookie.getValue();
         *         }
         *     }
         *     
         *     // 2. 根据 sessionId 从 Session 中获取用户信息
         *     HttpSession session = request.getSession(false); // false表示不创建新session
         *     Object userObj = session.getAttribute("user");
         *     
         *     // 3. 如果没有找到用户，说明未登录
         *     if (userObj == null) {
         *         throw new BusinessException("未登录");
         *     }
         *     
         *     // 4. 返回用户对象
         *     return (PictureUser) userObj;
         * }
         * ```
         * 
         * 💡 总结：
         * - Cookie 中存的是 sessionId（一把钥匙）
         * - Session 中存的是完整的用户信息（保险箱里的内容）
         * - 通过 sessionId 就能找到对应的用户信息
         */
        //当前登录用户（从 Session 中获取）
        UserEntity loginUser = userService.getLoginUser(request);
        
        // 将注解中要求的角色字符串转换为枚举类型
        UserRoleEnum mustUserRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (mustUserRoleEnum == null){
            throw new BusinessException(ResCodeEnum.NOT_AUTH_ERROR);
        }
        
        /**
         * 权限校验逻辑
         * 
         * 如果要求是管理员角色，但当前用户不是管理员
         * 则抛出"无权限"异常
         * 
         * 注意：这里只校验了管理员权限
         * 普通用户访问普通接口会直接放行
         */
        if (UserRoleEnum.ADMIN.equals(mustUserRoleEnum) && !UserRoleEnum.ADMIN.equals(loginUser.getUserRole())){
            throw new BusinessException(ResCodeEnum.NOT_AUTH_ERROR);
        }

        //通过权限校验，放行 → 执行原方法
        return joinPoint.proceed();
    }
}
```

### 6.5 使用示例

```java
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 删除用户（仅管理员可访问）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = "admin")  // ← 加上这个注解，自动进行权限校验
    public Result<Boolean> deleteUser(@RequestBody DeleteRequest request) {
        // 只有管理员才能执行到这里
        boolean result = userService.deleteUser(request.getId());
        return Result.success(result);
    }
    
    /**
     * 查询用户列表（所有登录用户都可访问）
     */
    @GetMapping("/list")
    public Result<List<PictureUser>> listUsers() {
        // 不需要 @AuthCheck，或者设置为普通用户角色
        List<PictureUser> users = userService.list();
        return Result.success(users);
    }
}
```

### 6.6 执行流程

```
用户访问 /user/delete 接口
         ↓
Spring AOP 检测到 @AuthCheck 注解
         ↓
执行 AuthInterceptor.doInterceptor()
         ↓
1. 从 RequestContextHolder 获取 HttpServletRequest
         ↓
2. 从 Cookie 中获取 sessionId
         ↓
3. 根据 sessionId 从 Session 中获取用户信息
         ↓
4. 检查用户角色是否为 admin
         ↓
    ┌────┴────┐
    ↓         ↓
是管理员   不是管理员
    ↓         ↓
执行原方法  抛出异常
    ↓         ↓
返回结果  返回 401 错误
```

---

## 7. 常见问题解答

### Q1: Cookie 和 Session 哪个更安全？

**A:** Session 更安全，因为：
- Session 数据存储在服务器端，用户无法直接访问
- Cookie 存储在浏览器端，用户可以查看和修改（虽然可以设置 HttpOnly 防止 JavaScript 访问）

**最佳实践：**
- Cookie 只存储 sessionId
- 敏感信息（如用户权限）存储在 Session 中

### Q2: Session 什么时候会失效？

**A:** Session 会在以下情况失效：
1. 超过最大不活动时间（默认 30 分钟）
2. 调用 `session.invalidate()` 主动销毁
3. 服务器重启（除非配置了 Session 持久化）

### Q3: 分布式系统如何处理 Session？

**A:** 常见方案：
1. **Session 复制**：多台服务器之间同步 Session（不推荐，性能差）
2. **Session 粘滞**：同一用户的请求总是路由到同一台服务器（负载均衡问题）
3. **集中式 Session 存储**：使用 Redis 等存储 Session（✅ 推荐）

### Q4: Token 和 Session 有什么区别？

**A:** 

| 特性 | Session | Token（JWT） |
|------|---------|-------------|
| 存储位置 | 服务器 | 客户端 |
| 扩展性 | 较差（占用服务器内存） | 较好（无状态） |
| 安全性 | 高 | 中等（需妥善保护） |
| 适用场景 | 传统 Web 应用 | 移动端、前后端分离 |

### Q5: 如何防止 Session 劫持？

**A:** 安全措施：
1. 使用 HTTPS 加密传输
2. 设置 Cookie 的 `HttpOnly` 属性（防止 XSS 攻击）
3. 设置 Cookie 的 `Secure` 属性（仅通过 HTTPS 传输）
4. 定期更换 sessionId
5. 登录后重新生成 sessionId

```java
// 设置安全的 Cookie
Cookie cookie = new Cookie("JSESSIONID", sessionId);
cookie.setHttpOnly(true);  // 防止 JavaScript 访问
cookie.setSecure(true);    // 仅通过 HTTPS 传输
cookie.setPath("/");
response.addCookie(cookie);
```

### Q6: ThreadLocal 会导致内存泄漏吗？

**A:** 会的！如果不当使用会导致内存泄漏。

**原因：**
- ThreadLocal 的值与线程生命周期绑定
- 线程池中的线程不会销毁
- 如果不手动清理，值会一直占用内存

**解决方案：**
```java
try {
    // 使用 ThreadLocal
    requestAttributes = ...;
} finally {
    // 务必清理
    RequestContextHolder.resetRequestAttributes();
}
```

### Q7: 为什么 AOP 中不能直接注入 HttpServletRequest？

**A:** 因为：
- AOP 切面不是 Controller，不在 Spring MVC 的请求处理链中
- Spring 只能在 Controller 方法参数中自动注入 request
- 需要使用 `RequestContextHolder` 从当前线程获取

---

## 📚 延伸阅读

### 相关技术栈

- **Spring Framework**: IoC、AOP、MVC
- **Servlet API**: HttpServletRequest、HttpSession、Cookie
- **Java 注解**: 元注解、自定义注解、反射
- **Spring Boot**: 自动配置、 starter

### 推荐学习资源

1. 《Head First Servlets and JSP》- 深入理解 Web 开发基础
2. 《Spring in Action》- Spring 框架权威指南
3. Oracle 官方文档 - Java Annotations Tutorial
4. Spring 官方文档 - AOP Reference

---

## 🎯 总结

### 核心要点回顾

```
✅ HTTP 是无状态协议，需要 Session + Cookie 实现会话管理
✅ Cookie 存储在浏览器，Session 存储在服务器
✅ Cookie 中存储 sessionId，Session 中存储用户完整信息
✅ 自定义注解需要 @Target 和 @Retention 元注解
✅ AOP 可以实现统一的权限校验，避免代码重复
✅ RequestContextHolder 基于 ThreadLocal，可在任何地方获取 request
```

### 学习建议

1. **先理解概念**：搞清楚 Cookie、Session、登录态的关系
2. **动手实践**：自己写一个简单的登录功能
3. **阅读源码**：看看 Spring Security 是如何实现的
4. **深入理解**：学习 AOP、反射、ThreadLocal 等底层原理

---

**文档版本**: v1.0  
**最后更新**: 2024-01-01  
**作者**: AI Assistant  

---

*希望这份文档能帮助你理解 Java Web 中的登录态管理！如有疑问，欢迎随时提问。* 😊
