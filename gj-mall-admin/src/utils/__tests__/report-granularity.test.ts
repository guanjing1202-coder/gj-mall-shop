import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  formatReportPeriod,
  normalizeReportGranularity,
  reportGranularityOptions,
} from '../report-granularity.ts'

describe('report-granularity', () => {
  it('normalizes unsupported values to day', () => {
    assert.equal(normalizeReportGranularity('week'), 'week')
    assert.equal(normalizeReportGranularity('MONTH'), 'month')
    assert.equal(normalizeReportGranularity('quarter'), 'day')
    assert.equal(normalizeReportGranularity(), 'day')
  })

  it('formats period labels by granularity', () => {
    assert.equal(formatReportPeriod('2026-05-21', 'day'), '05-21')
    assert.equal(formatReportPeriod('2026-05-11', 'week'), '05-11 周')
    assert.equal(formatReportPeriod('2026-05-01', 'month'), '2026-05')
    assert.equal(formatReportPeriod('', 'month'), '--')
  })

  it('exposes stable admin selector options', () => {
    assert.deepEqual(reportGranularityOptions, [
      { label: '按日', value: 'day' },
      { label: '按周', value: 'week' },
      { label: '按月', value: 'month' },
    ])
  })
})
