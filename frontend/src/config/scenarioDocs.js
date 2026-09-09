// 场景技术文档注册表：md 经 ?raw 内联、SVG 经 ?url 打包为资源
// 新场景在此登记：场景设计文档（design.md）+ md 内相对图片路径 → 导入 URL 的映射
import designMd from '../../../scenarios/01-auth/design.md?raw'
import archSvg from '../../../scenarios/01-auth/auth-jwt/auth-architecture.svg?url'
import flowSvg from '../../../scenarios/01-auth/auth-jwt/dual-token-flow.svg?url'
import jwtSvg from '../../../scenarios/01-auth/auth-jwt/jwt-structure.svg?url'
import identitySvg from '../../../scenarios/01-auth/auth-jwt/identity-session-vs-jwt.svg?url'
import refreshSequenceSvg from '../../../scenarios/01-auth/auth-jwt/token-refresh-sequence.svg?url'
import singleFlightSvg from '../../../scenarios/01-auth/auth-jwt/refresh-single-flight.svg?url'
import rotationSvg from '../../../scenarios/01-auth/auth-jwt/refresh-rotation-boundary.svg?url'
import storageSecuritySvg from '../../../scenarios/01-auth/auth-jwt/token-storage-security.svg?url'
import verificationSvg from '../../../scenarios/01-auth/auth-jwt/refresh-verification-map.svg?url'
import guaranteesSvg from '../../../scenarios/01-auth/auth-jwt/auth-guarantees-summary.svg?url'
import flashDesignMd from '../../../scenarios/03-flash-sale/design.md?raw'
import flashArchSvg from '../../../scenarios/03-flash-sale/diagrams/flash-sale-architecture.svg?url'
import flashGuardSvg from '../../../scenarios/03-flash-sale/diagrams/oversell-guard.svg?url'
import flashGuaranteesSvg from '../../../scenarios/03-flash-sale/diagrams/flash-sale-guarantees.svg?url'
import flashRollbackSvg from '../../../scenarios/03-flash-sale/diagrams/unique-order-rollback.svg?url'
import flashMarkerSvg from '../../../scenarios/03-flash-sale/diagrams/sold-out-marker-boundary.svg?url'
import flashObservationSvg from '../../../scenarios/03-flash-sale/diagrams/race-observation-model.svg?url'
import flashResetSvg from '../../../scenarios/03-flash-sale/diagrams/demo-reset-boundary.svg?url'
import flashDataSvg from '../../../scenarios/03-flash-sale/diagrams/flash-data-contract.svg?url'
import flashVerificationSvg from '../../../scenarios/03-flash-sale/diagrams/flash-verification-matrix.svg?url'

export const scenarioDocs = {
  '01-auth': {
    markdown: designMd,
    assets: {
      'auth-jwt/identity-session-vs-jwt.svg': identitySvg,
      'auth-jwt/auth-architecture.svg': archSvg,
      'auth-jwt/dual-token-flow.svg': flowSvg,
      'auth-jwt/token-refresh-sequence.svg': refreshSequenceSvg,
      'auth-jwt/refresh-single-flight.svg': singleFlightSvg,
      'auth-jwt/refresh-rotation-boundary.svg': rotationSvg,
      'auth-jwt/jwt-structure.svg': jwtSvg,
      'auth-jwt/token-storage-security.svg': storageSecuritySvg,
      'auth-jwt/refresh-verification-map.svg': verificationSvg,
      'auth-jwt/auth-guarantees-summary.svg': guaranteesSvg,
    },
  },
  '03-flash-sale': {
    markdown: flashDesignMd,
    assets: {
      'diagrams/flash-sale-guarantees.svg': flashGuaranteesSvg,
      'diagrams/flash-sale-architecture.svg': flashArchSvg,
      'diagrams/oversell-guard.svg': flashGuardSvg,
      'diagrams/unique-order-rollback.svg': flashRollbackSvg,
      'diagrams/sold-out-marker-boundary.svg': flashMarkerSvg,
      'diagrams/race-observation-model.svg': flashObservationSvg,
      'diagrams/demo-reset-boundary.svg': flashResetSvg,
      'diagrams/flash-data-contract.svg': flashDataSvg,
      'diagrams/flash-verification-matrix.svg': flashVerificationSvg,
    },
  },
}
