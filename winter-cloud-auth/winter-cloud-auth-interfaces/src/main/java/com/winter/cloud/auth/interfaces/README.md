职责：系统的入口。解析参数，调用应用层。


controller : HTTP 接口 (e.g., DictController)

facade : Dubbo/Feign 接口的实现类 (实现了 api 模块定义的接口)

job : 定时任务 (e.g., DictSyncJob) 

listener (或 consumer) : MQ 消费者 (e.g., UserLoginListener)

assembler : 组装器。将 Web 请求参数(VO) 转换为 App 层的 Command/Query。