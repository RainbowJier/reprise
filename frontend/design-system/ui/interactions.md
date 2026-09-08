# UI · 交互与可访问性边界

> 本页描述源码中实际存在的行为；未再次进行浏览器、读屏或完整键盘测试。功能存在与验收合规是不同结论。

## 1. 应用导航

来源：[AdminLayout.vue](../../src/layouts/AdminLayout.vue)、[router/index.js](../../src/router/index.js)。

### 桌面目录

- 展开状态是单个 expandedId，同一时间最多展开一个已收录场景。
- 当前场景按已启用项的演示路径或精确 `/doc` 路径匹配。
- 路由 path 改变时收起移动抽屉和账户菜单，同时展开当前场景。
- 单纯 query/hash 变化不触发该 path watcher。
- 规划项为静态目录条目，不能导航。

### 手机抽屉

已有：

- 触发按钮有名称、aria-expanded 与 aria-controls。
- 打开时锁定 body 滚动，焦点移到关闭按钮。
- 打开态设置 dialog / aria-modal 与可访问名称。
- Tab 与 Shift+Tab 在首尾可聚焦控件间循环。
- 关闭按钮、遮罩或 Escape 可关闭，显式关闭时把焦点还给触发按钮。
- 路由切换关闭抽屉，组件卸载清理 body overflow。

边界：

- 没有背景 inert，也没有全局 focus-in 越界拦截。
- 路由切换关闭后没有显式聚焦新页标题或 main。
- body overflow 清理为默认空值，不恢复打开前的自定义值。
- 没有响应断点变化自动重置抽屉状态。

因此可写“具备基本焦点循环和 Escape 关闭”，不能写“完整可复用无障碍 Modal”。

### 账户菜单

- 点击按钮切换，点击遮挡层或 Escape 关闭；路由切换关闭。
- 退出清除本地认证状态并进入登录页。
- 没有 menu/menuitem 语义、方向键菜单导航、打开后的自动聚焦或 Escape 后的焦点恢复。
- 这是一组可用原生按钮构成的简易弹层，不是 ARIA Menu 组件。

### 路由与滚动

- 首页、技术文档、场景演示均需要已有 accessToken；login/register 不要求。
- 路由守卫只检查 token 存在，不验证有效期或服务端签名。
- 未登录回跳参数保留 fullPath；未知地址重定向首页，没有独立 404。
- 已登录访问 login/register 不会自动离开该页面。
- 滚动依次采用 savedPosition、hash 元素（top 110）、页首。
- 页面标题根据 meta 或账号页面名称更新；没有全局路由后焦点迁移。

## 2. 首页搜索与筛选

来源：[HomeView.vue](../../src/views/HomeView.vue)。

- 默认全部、关键词为空。
- 本地匹配编号、名称、描述和 tags；trim、忽略大小写，不直接搜索 category。
- 默认已收录优先，其他维持元信息顺序。
- 筛选栏数量是全集统计；结果区隐藏的 polite 文案表达实际匹配数。
- 搜索与状态不写入 URL，不持久化；离开再进入页面会回到初始状态。
- 清除筛选同时清空关键词并选中全部。
- 已收录卡片有独立文档/演示链接；整张卡片没有点击处理。

可访问性：搜索有名称；状态按钮有 aria-pressed；结果数有 live region。没有搜索建议下拉、全站命令面板或键盘快捷键。

## 3. 登录与注册

来源：[LoginView.vue](../../src/views/LoginView.vue)、[RegisterView.vue](../../src/views/RegisterView.vue)、[auth.js](../../src/stores/auth.js)。

### 共有状态

`初始输入 → 本地校验 → 请求中 → 成功导航 / 内联错误`。

- 原生 form + submit.prevent，字段有 label、required、autocomplete。
- 请求中禁用提交按钮、显示 loading 文案与旋转指示；没有禁用所有输入。
- 错误 role=alert；没有全局 toast 或 aria-busy。
- 用户名 trim，密码保留原输入。
- 成功 redirect 只接受以 `/` 开头且不以 `//` 开头的字符串，否则回首页。
- 登录/注册互相切换保留 query。
- 显示密码按钮改变 input type，具备动态名称和 aria-pressed。

### 登录

- 校验用户名和密码非空。
- 错误时两个字段均 aria-invalid，并关联 login-error。
- 演示账号按钮填入 demo / demo123456 并清空错误，不自动提交。

### 注册

- 用户名 4–32 位字母/数字/下划线；密码 6–64 位；确认密码需相同。
- 原生约束与提交前校验共同存在。
- 一个开关同时控制密码/确认密码显隐。
- 昵称可选，无前端长度限制，空昵称转为 undefined。
- 成功后即登录，不先跳登录页。
- 当前未对单个字段设置 aria-invalid/aria-describedby，不具备与登录页完全相同的错误关联。

共同边界：没有首个错误字段自动聚焦、集中字段错误对象或通用 FormField。注册成功流程会创建真实账号，不能为视觉检查随意批量提交。

## 4. 认证演示与请求层

来源：[AuthView.vue](../../src/views/scenario/AuthView.vue)、[http.js](../../src/api/http.js)、[counter.js](../../src/stores/counter.js)。

- 挂载时自动健康检查，身份接口只在点击后执行。
- 身份与健康独立 loading/error；不显示全页阻塞层。
- 身份请求前清空前次结果，成功显示 id/username/nickname，不显示 token 原文。
- 请求按钮禁用，结果区 polite；错误 alert。
- 计数器 +1 仅更新 Pinia，当前 SPA 实例中保持；整页刷新重新初始化，未持久化。
- 页面“静默刷新”说明对应统一请求层，不是假的流程动画。

请求层仅在 HTTP 成功响应内 `code=401` 且有 refreshToken 时尝试共享刷新，然后最多重放一次原请求；不是任意 HTTP 401 都自动刷新。

## 5. 文档阅读

来源：[DocView.vue](../../src/views/scenario/DocView.vue)、[renderDoc.js](../../src/utils/renderDoc.js)、[scenarioDocs.js](../../src/config/scenarioDocs.js)。

### 内容来源

- 静态导入仓库 design.md 与对应 SVG，不运行时请求文档接口。
- 缺失文档显示无文档信息，没有网络加载状态。
- 所有 h1 省略；h2–h6 顺序编号，只有 h2/h3 进入目录。
- id 对同一输入稳定，不是标题 slug；插入标题会改变后续编号。
- 资源映射是精确 Markdown 链接字符串替换，不是通用 URL 解析器。

### 阅读反馈

- 预计阅读时间为 Markdown 字符数 / 700 向上取整，至少 1 分钟，不是词数/难度模型。
- scroll/resize 经 requestAnimationFrame 更新。
- 高亮选取 top≤150px 的最后一个 h2/h3；不是 IntersectionObserver。
- 进度按正文几何位置估算并限制 0–100；桌面显示，没有 progressbar 语义。
- 桌面目录 link 有 aria-current=location；手机 details 目录没有当前章节样式状态，点击也不会自动折叠。
- 初始 hash 的 scrollIntoView、标题 scroll-mt-28、路由 hash top 110 均存在，不能说只有一个统一偏移。

### 代码复制

- 按钮编号名称明确，通过正文事件委托访问对应代码原文。
- Clipboard API 成功/失败均有真实反馈；没有旧式 execCommand fallback。
- 反馈 role=status，3 秒消失；是 DocView 私有状态，不是全局通知系统。
- 没有语法高亮、行号、折叠。

### 安全与语义边界

- 只用于仓库受信 Markdown，没有全文 HTML sanitizer 或外部 URL 白名单。
- 代码块转义不能替代全文消毒。
- 表格采用原生 table/thead/th，但没有 caption/scope；固定左对齐，不使用 Markdown 列对齐信息。
- 横向滚动容器没有额外键盘入口，不能据此宣称宽表格已完成完整可访问性测试。

## 6. 秒杀实验

来源：[FlashSaleView.vue](../../src/views/scenario/FlashSaleView.vue)、[flashRace.js](../../src/api/flashRace.js)。

### 商品与订单

- 初始化加载商品和订单；1 秒更新倒计时，默认每 2 秒刷新商品。
- 自动刷新不包含订单；订单在抢购成功、实验结束、重置等动作后加载。
- 倒计时只是显示，按钮依据服务端 status、mine、stock。
- 禁用优先级：该商品请求中、已抢购、未开始、已结束、已售罄。
- busyItemId 是单值，不是全页互斥锁或多商品 busy 集合。
- 列表 skeleton/空/错、订单错误重试、抢购结果反馈独立呈现。
- 金额从分转换为 ¥；库存/统计条当前没有 progressbar 语义。

### 并发实验

| 项目 | 当前行为 |
| --- | --- |
| 默认模式 | 多用户洪峰 storm |
| 默认数量 | 24，限制 4–50 |
| 账号准备 | 每批 8 个并发注册真实虚拟账号 |
| 起跑差 | 随机 0–400ms |
| 单用户模式 | 当前登录用户固定 12 个并发请求 |
| 目标 | 优先进行中 id=2，否则首个进行中商品 |
| 日志 | 随响应追加，最多 100 条 |
| 统计 | 实验结束设置汇总，不是不断刷新的统计对象 |
| 核对 | 初始库存 − 本批成功数与服务器库存比较 |

实验中锁定相关实验控件，但不锁定页面所有普通商品操作；卸载清理定时器，不取消已经发出的请求/实验。

模式切换按钮没有 aria-pressed；日志、实验阶段和汇总没有 live region。实验核对不能作为生产级压测证明或防超卖形式化保证。

### 库存重置

- 打开内联危险确认面板，明确商品名、回满库存和永久清空该商品订单。
- 确认调用真实后端；取消只关闭面板。
- 重置中锁定相关实验控件，完成后刷新商品和订单。
- 不是 modal/alertdialog，不带焦点约束。

实际抢购、注册虚拟账号、确认重置均会修改数据。文档生成/静态视觉检查不能默认执行这些操作。

## 7. 后续可访问性补强候选（未实现）

- 路由跳转后的 main/标题焦点管理。
- 账户弹层的明确键盘模型与焦点恢复。
- 抽屉背景 inert、断点切换状态恢复。
- 注册字段错误关联、提交区 aria-busy。
- 实验模式的选中语义、适度节流的状态播报。
- 进度条语义、宽表格标题与键盘访问。

这些是可单独设计的后续任务，不作为当前组件功能对外承诺。
