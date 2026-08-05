# getQueryWrapper vs XML 写 SQL

> 对应项目：`mo-picture-backend`  
> 参考实现：`UserServiceImpl#getQueryWrapper`、`PicturePictureServiceImpl#getQueryWrapper`  
> （`PictureSpaceServiceImpl#getQueryWrapper` 同属此模式）

---

## 一、结论（十年经验怎么选）

**不会二选一死磕**，按场景选：

| 场景 | 更合适 |
|------|--------|
| 列表筛选、分页、多条件动态查询 | **`getQueryWrapper`（MyBatis-Plus Wrapper）** |
| 复杂 SQL、多表 JOIN、报表、窗口函数、手工优化执行计划 | **XML / 注解 SQL** |

本项目的用户/图片/空间「分页列表 + 多条件筛选」属于前者，抽 `getQueryWrapper` 符合常见 Java 业务项目做法。

---

## 二、getQueryWrapper 在干什么

把前端查询 DTO（如 `PictureQueryRequest` / `SpaceQueryRequest`）转成 MyBatis-Plus 的条件对象，再交给通用 `page()` / `list()` / `count()` 执行。

示例（图片）：

```java
public QueryWrapper<PictureEntity> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
    QueryWrapper<PictureEntity> queryWrapper = new QueryWrapper<>();
    if (pictureQueryRequest == null) {
        return queryWrapper;
    }
    // 有值才拼条件
    queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
    queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
    // ...
    queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
    return queryWrapper;
}
```

Controller 调用：

```java
picturePictureService.page(
    new Page<>(current, size),
    picturePictureService.getQueryWrapper(pictureQueryRequest)
);
```

本质：**条件组装集中在一处，查询执行复用 MP 通用方法**。

---

## 三、为什么要单独写 getQueryWrapper（好处）

1. **复用**  
   管理端分页、用户端列表、导出、count 共用同一套条件，避免每个接口复制 `if (name != null)`。

2. **动态条件干净**  
   `eq(condition, column, val)` 自带“有值才拼”，少写 XML 长串 `<if test="...">`。

3. **职责清晰**  
   Controller 管入参/分页；Service 管“怎么筛”；Mapper XML 不必为每个列表再堆一份动态 SQL。

4. **贴合 MyBatis-Plus**  
   直接吃 `page()`、`list()`、`count()`，少写样板 Mapper 方法。

5. **简单查询改字段成本低**  
   多一个筛选条件，Java 加一行即可，不必同步改 XML `<where>`。

---

## 四、和 XML 对比

| 维度 | getQueryWrapper | Mapper XML |
|------|-----------------|------------|
| 单表动态筛选 | 很合适 | 也能写，但偏啰嗦 |
| 多表 JOIN / 子查询 | 易变丑、难控 | 更清晰 |
| SQL 可读性 / 执行计划优化 | 较弱 | 更强 |
| 与 MP 分页集成 | 开箱即用 | 需自定义方法 |
| 字段名安全 | 字符串列名易写错（建议 LambdaWrapper） | 手写列名，同样要小心 |
| 复用条件 | 抽方法即可 | 可用 `<sql>` / `<include>` |

### Wrapper 的代价（要知道）

- 列名常写成魔法字符串（如 `"userId"`），重构不友好 → 更推荐 `LambdaQueryWrapper`
- 复杂 OR / 嵌套条件可读性一般
- 极致 SQL 优化、强制索引等，不如手写 XML

---

## 五、等价的 XML 动态 SQL 长什么样

若不用 Wrapper，大致要写：

```xml
<select id="listByQuery" resultMap="BaseResultMap">
  SELECT ...
  FROM picture_picture
  <where>
    <if test="id != null">AND id = #{id}</if>
    <if test="name != null and name != ''">AND name LIKE CONCAT('%', #{name}, '%')</if>
    <if test="userId != null">AND userId = #{userId}</if>
    <!-- 每个条件一个 if，列表每增一个筛选项就改 XML -->
  </where>
  <if test="sortField != null and sortField != ''">
    ORDER BY ${sortField} <!-- 注意 SQL 注入风险，需白名单 -->
  </if>
</select>
```

简单列表用 Wrapper 更省事；SQL 一旦复杂，XML 往往更可控。

---

## 六、十年开发的取舍口诀

> **简单动态查询放 Wrapper；复杂关联与性能敏感 SQL 放 XML。**  
> `getQueryWrapper` 不是炫技，而是把「动态查询条件」收成可复用构建器，让列表接口少写重复 if / XML。

### 生产建议

1. 优先 `LambdaQueryWrapper` / `lambdaQuery()`，减少字符串列名
2. 排序字段做**白名单**，禁止前端随意传列名拼进 `orderBy`
3. 多表、报表、复杂统计：下沉 Mapper XML
4. 同一套条件被多处使用时，务必抽 `getQueryWrapper`（或独立 QueryBuilder）

---

## 七、相关面试点（可背）

1. MyBatis-Plus `QueryWrapper` 和手写 XML 各自适用场景？  
2. `@Transactional` / 分页插件如何与 Wrapper 配合？  
3. 动态 `ORDER BY` 的 SQL 注入风险怎么防？  
4. 为什么 Conditon 形式的 `eq(boolean, column, val)` 比先 if 再 eq 更简洁？  
5. `LambdaQueryWrapper` 相比普通 `QueryWrapper` 的优势？
