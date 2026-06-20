package com.mobai.mopicturebackend.aop;

import com.mobai.mopicturebackend.annotation.AuthCheck;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.enums.UserRoleEnum;
import com.mobai.mopicturebackend.service.UserService;
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
 * <p>
 * ============================================================================
 * 【小白必读】登录态、Session、Cookie 详解
 * ============================================================================
 * <p>
 * 🌐 HTTP协议的特点：
 * - HTTP是"无状态"协议，每次请求都是独立的
 * - 服务器不知道两次请求是否来自同一个用户
 * - 就像你去银行，每次取钱柜员都不认识你，需要重新出示身份证
 * <p>
 * 🔑 解决方案：会话管理（Session + Cookie）
 * <p>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    完整的登录流程                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │                                                              │
 * │  1️⃣ 用户登录                                                │
 * │     浏览器 → [POST /login] → 服务器                         │
 * │     {username: "admin", password: "123456"}                │
 * │                                                              │
 * │  2️⃣ 服务器验证通过后                                         │
 * │     - 创建 Session（会话），存储在服务器内存中               │
 * │     - Session 包含：sessionId, userId, userRole 等信息      │
 * │     - 生成一个唯一的 sessionId（如：abc123xyz）              │
 * │                                                              │
 * │  3️⃣ 服务器响应浏览器                                         │
 * │     Set-Cookie: JSESSIONID=abc123xyz                        │
 * │     （通过响应头告诉浏览器保存这个Cookie）                    │
 * │                                                              │
 * │  4️⃣ 浏览器自动保存 Cookie                                    │
 * │     Cookie 存储在浏览器中，内容：JSESSIONID=abc123xyz       │
 * │                                                              │
 * │  5️⃣ 后续每次请求                                             │
 * │     浏览器自动在请求头带上：                                  │
 * │     Cookie: JSESSIONID=abc123xyz                            │
 * │                                                              │
 * │  6️⃣ 服务器收到请求                                           │
 * │     - 从 Cookie 中取出 sessionId                            │
 * │     - 根据 sessionId 查找对应的 Session                      │
 * │     - 从 Session 中获取用户信息（userId, userRole等）        │
 * │     - 这就是"登录态"！                                       │
 * │                                                              │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * 📦 核心概念对比：
 * <p>
 * ┌──────────┬────────────────┬────────────────┐
 * │          │   Cookie       │   Session      │
 * ├──────────┼────────────────┼────────────────┤
 * │ 存储位置 │ 浏览器（客户端）│ 服务器          │
 * │ 安全性   │ 较低（可被篡改）│ 较高            │
 * │ 容量     │ 小（4KB左右）  │ 大              │
 * │ 作用     │ 存储sessionId  │ 存储用户完整信息 │
 * │ 生命周期 │ 可设置过期时间  │ 默认30分钟超时  │
 * └──────────┴────────────────┴────────────────┘
 * <p>
 * 💡 通俗比喻：
 * - Cookie 像是"会员卡号"，存在你钱包里（浏览器）
 * - Session 像是"会员档案"，存在商场数据库里（服务器）
 * - 每次去商场，你出示会员卡号（Cookie中的sessionId）
 * - 商场根据卡号查到你的档案（Session），知道你是谁、有什么权限
 * <p>
 * 🔍 本代码的作用：
 * 1. 拦截带有 @AuthCheck 注解的方法
 * 2. 从请求中获取当前登录用户信息（通过 Session）
 * 3. 检查用户是否有足够的权限
 * 4. 权限不足则抛出异常，权限足够则放行
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 权限校验拦截方法（环绕通知）
     *
     * @Around 说明：
     * - 在目标方法执行前后都可以插入逻辑
     * - 可以决定是否执行目标方法（joinPoint.proceed()）
     * - "@annotation(authCheck)" 表示拦截所有带有 @AuthCheck 注解的方法
     * <p>
     * 执行流程：
     * 1. 用户访问某个接口（如：/user/delete）
     * 2. 该接口方法上有 @AuthCheck(mustRole = "admin")
     * 3. Spring AOP 拦截到这个调用，先执行 doInterceptor 方法
     * 4. 进行权限校验
     * 5. 校验通过 → 执行原方法（joinPoint.proceed()）
     * 6. 校验失败 → 抛出异常，原方法不会执行
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
         *
         * 🔧 代码解析（第31-32行）：
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
        if (mustUserRoleEnum == null) {
            throw new BusinessException(ResCodeEnum.NOT_AUTH_ERROR);
        }

        /**
         * 权限校验逻辑
         *
         * 如果要求是管理员角色，但当前用户不是管理员
         * 则抛出"无权限"异常
         *
         * 注意：这里只校验了管理员权限
         * 普通用户访问普通接口会直接放行（第44行）
         */
        if (UserRoleEnum.ADMIN.equals(mustUserRoleEnum) && !UserRoleEnum.ADMIN.getRoleValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ResCodeEnum.NOT_AUTH_ERROR);
        }

        //通过权限校验，放行 → 执行原方法
        return joinPoint.proceed();

    }
}
