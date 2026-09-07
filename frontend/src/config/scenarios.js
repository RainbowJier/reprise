// 场景菜单清单：与 README 场景索引保持一致
// enabled = 已在本仓库实现（路由可达）；未实现的场景在侧栏置灰展示为「规划中」
// 扩展点：接入后端菜单/权限下发后，本文件可改为接口数据驱动
export const scenarios = [
  { id: '01-auth', no: '01', name: '用户登录与认证', desc: 'JWT 双 token 无感续期', path: '/scenario/01-auth', enabled: true },
  { id: '02-short-link', no: '02', name: '短链接服务', path: '/scenario/02-short-link', enabled: false },
  { id: '03-flash-sale', no: '03', name: '秒杀抢购', desc: '高并发防超卖三层防线', path: '/scenario/03-flash-sale', enabled: true },
  { id: '04-feed', no: '04', name: 'Feed 流', path: '/scenario/04-feed', enabled: false },
  { id: '05-im', no: '05', name: '即时通讯', path: '/scenario/05-im', enabled: false },
  { id: '06-upload', no: '06', name: '文件上传', path: '/scenario/06-upload', enabled: false },
  { id: '07-payment', no: '07', name: '支付回调对账', path: '/scenario/07-payment', enabled: false },
  { id: '08-rbac', no: '08', name: 'RBAC 权限', path: '/scenario/08-rbac', enabled: false },
  { id: '09-distributed-lock', no: '09', name: '分布式锁', path: '/scenario/09-distributed-lock', enabled: false },
  { id: '10-caching', no: '10', name: '多级缓存', path: '/scenario/10-caching', enabled: false },
]
