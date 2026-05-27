import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  canInitializePaymentConfig,
  formatConfigInitResult,
  isMaskedConfigValue,
  isSensitiveConfig,
  normalizeConfigSummaryMetrics,
  sensitiveConfigHint,
  shouldConfirmSensitiveConfigSave,
} from '../system-config-ui.ts'

describe('system-config-ui', () => {
  it('detects payment secret config keys as sensitive', () => {
    assert.equal(isSensitiveConfig({ configKey: 'mall.pay.callback.secret' }), true)
    assert.equal(isSensitiveConfig({ configKey: 'mall.pay.wechat.api-v3-key' }), true)
    assert.equal(isSensitiveConfig({ configKey: 'mall.site.name' }), false)
  })

  it('treats masked values as unchanged placeholders', () => {
    assert.equal(isMaskedConfigValue('pr***************et'), true)
    assert.equal(isMaskedConfigValue('new-prod-secret'), false)
  })

  it('asks for confirmation only when a sensitive value is replaced', () => {
    const record = { configKey: 'mall.pay.callback.secret', sensitive: true, masked: true }

    assert.equal(shouldConfirmSensitiveConfigSave(record, { configValue: 'pr***************et' }), false)
    assert.equal(shouldConfirmSensitiveConfigSave(record, { configValue: 'new-prod-secret' }), true)
  })

  it('asks for confirmation when creating a new sensitive config', () => {
    assert.equal(
      shouldConfirmSensitiveConfigSave(undefined, {
        configKey: 'mall.pay.wechat.private-key-path',
        configValue: '/secure/wx-private.pem',
      }),
      true
    )
  })

  it('explains masked sensitive values', () => {
    assert.match(
      sensitiveConfigHint({ configKey: 'mall.pay.callback.secret', sensitive: true, masked: true }),
      /保留原始密钥/
    )
  })

  it('normalizes config summary metrics for readiness panels', () => {
    assert.deepEqual(
      normalizeConfigSummaryMetrics({
        totalCount: 3,
        requiredCount: 12,
        readyCount: 4,
        enabledCount: 5,
        missingCount: 8,
      }),
      {
        requiredCount: 12,
        readyCount: 4,
        enabledCount: 5,
        missingCount: 8,
      }
    )

    assert.deepEqual(normalizeConfigSummaryMetrics({ totalCount: 3 }), {
      requiredCount: 3,
      readyCount: 0,
      enabledCount: 0,
      missingCount: 0,
    })
  })

  it('shows payment config initialization only when payment keys are missing', () => {
    assert.equal(canInitializePaymentConfig({ groupCode: 'payment', missingCount: 2, uninitializedCount: 2 }), true)
    assert.equal(canInitializePaymentConfig({ groupCode: 'payment', missingCount: 2, uninitializedCount: 0 }), false)
    assert.equal(canInitializePaymentConfig({ groupCode: 'payment', missingCount: 0, uninitializedCount: 0 }), false)
    assert.equal(canInitializePaymentConfig({ groupCode: 'order', missingCount: 2, uninitializedCount: 2 }), false)
  })

  it('formats config initialization results with created and restored counts', () => {
    assert.equal(
      formatConfigInitResult({ createdCount: 10, restoredCount: 2, existingCount: 0 }),
      '支付配置已初始化：创建 10 项，恢复 2 项'
    )
    assert.equal(
      formatConfigInitResult({ createdCount: 0, restoredCount: 0, existingCount: 12 }),
      '支付配置已初始化：保留 12 项'
    )
    assert.equal(formatConfigInitResult({}), '支付配置已是最新')
  })
})
