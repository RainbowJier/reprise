// 场景技术文档注册表：md 经 ?raw 内联、SVG 经 ?url 打包为资源
// 新场景在此登记：场景设计文档（design.md）+ md 内相对图片路径 → 导入 URL 的映射
import designMd from '../../../scenarios/01-auth/design.md?raw'
import archSvg from '../../../scenarios/01-auth/auth-jwt/auth-architecture.svg?url'
import flowSvg from '../../../scenarios/01-auth/auth-jwt/dual-token-flow.svg?url'
import jwtSvg from '../../../scenarios/01-auth/auth-jwt/jwt-structure.svg?url'
import flashDesignMd from '../../../scenarios/03-flash-sale/design.md?raw'
import flashArchSvg from '../../../scenarios/03-flash-sale/diagrams/flash-sale-architecture.svg?url'
import flashGuardSvg from '../../../scenarios/03-flash-sale/diagrams/oversell-guard.svg?url'

export const scenarioDocs = {
  '01-auth': {
    markdown: designMd,
    assets: {
      'auth-jwt/auth-architecture.svg': archSvg,
      'auth-jwt/dual-token-flow.svg': flowSvg,
      'auth-jwt/jwt-structure.svg': jwtSvg,
    },
  },
  '03-flash-sale': {
    markdown: flashDesignMd,
    assets: {
      'diagrams/flash-sale-architecture.svg': flashArchSvg,
      'diagrams/oversell-guard.svg': flashGuardSvg,
    },
  },
}
