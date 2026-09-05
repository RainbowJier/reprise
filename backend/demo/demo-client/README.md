# demo-client

对外客户端契约层（对应 fjgtkj-2026 各服务的 `-client` 模块）：

- `client/service/`：本服务内部服务接口，实现位于 `demo-application`。
- `client/feign/`（预留）：本服务对外的 Feign 契约。demo 为单服务、无注册中心，
  暂不引入 `spring-cloud-starter-openfeign`；接入微服务时按 fjgtkj 模式在此定义
  `@FeignClient` 接口（返回 `AjaxResult<T>`），并在 infrastructure 的 Gateway
  实现中调用外部 Feign，application 层禁止直接注入他服务 client。
