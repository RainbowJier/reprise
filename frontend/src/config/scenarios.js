// enabled 仅在演示路由与技术文档均可用时开启，编号与 README 场景索引保持一致。
export const scenarios = [
  { id: '01-auth', no: '01', name: '用户登录与认证', desc: '从一次登录开始，理解 JWT 双令牌、无感续期与身份认证的安全边界。', tags: ['JWT', '安全', '身份认证'], category: '基础能力', icon: 'lock', path: '/scenario/01-auth', enabled: true },
  { id: '02-short-link', no: '02', name: '短链接服务', desc: '把长地址变短，探索编码、重定向与热点访问。', tags: ['编码', '重定向'], category: '基础能力', icon: 'arrow', path: '/scenario/02-short-link', enabled: false },
  { id: '03-flash-sale', no: '03', name: '秒杀抢购', desc: '在并发请求中守住库存，验证原子扣减、唯一索引与防超卖三层防线。', tags: ['高并发', '库存', '幂等'], category: '交易系统', icon: 'bolt', path: '/scenario/03-flash-sale', enabled: true },
  { id: '04-feed', no: '04', name: 'Feed 流', desc: '推、拉与推拉结合，理解信息流的读写取舍。', tags: ['信息流', '读写模型'], category: '内容与通信', icon: 'grid', path: '/scenario/04-feed', enabled: false },
  { id: '05-im', no: '05', name: '即时通讯', desc: '从长连接到消息送达，构建实时通信链路。', tags: ['WebSocket', '消息'], category: '内容与通信', icon: 'code', path: '/scenario/05-im', enabled: false },
  { id: '06-upload', no: '06', name: '文件上传', desc: '大文件分片、断点续传与秒传的实现路径。', tags: ['分片', '断点续传'], category: '基础能力', icon: 'arrow', path: '/scenario/06-upload', enabled: false },
  { id: '07-payment', no: '07', name: '支付回调对账', desc: '用幂等与对账，让异步支付结果最终可信。', tags: ['支付', '一致性'], category: '交易系统', icon: 'check', path: '/scenario/07-payment', enabled: false },
  { id: '08-rbac', no: '08', name: 'RBAC 权限', desc: '从角色到资源，组织清晰可维护的权限边界。', tags: ['权限', '安全'], category: '基础能力', icon: 'lock', path: '/scenario/08-rbac', enabled: false },
  { id: '09-distributed-lock', no: '09', name: '分布式锁', desc: '理解互斥、租约与跨进程并发控制的边界。', tags: ['并发控制', '分布式'], category: '系统设计', icon: 'lock', path: '/scenario/09-distributed-lock', enabled: false },
  { id: '10-caching', no: '10', name: '多级缓存', desc: '在响应速度与数据一致性之间做工程取舍。', tags: ['缓存', '一致性'], category: '系统设计', icon: 'bolt', path: '/scenario/10-caching', enabled: false },
]
