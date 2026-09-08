import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { renderDoc } from './renderDoc.js'

test('章节编号稳定且不会把代码内的标题当作目录', () => {
  const result = renderDoc('# 主标题\n## 章节\n#### 细节\n## 同名章节\n```md\n## 代码标题\n```')
  assert.equal(result.headings.length, 2)
  assert.deepEqual(result.headings.map(h => h.id), ['section-1', 'section-3'])
  assert.equal(result.codes[0], '## 代码标题')
  assert.ok(!result.html.includes('<h1'))
  assert.ok(result.html.includes('data-copy-code="0"'))
})

test('代码内容转义且保留原文供复制', () => {
  const code = '<script>alert("x")</script>'
  const result = renderDoc(`\`\`\`html\n${code}\n\`\`\``)
  assert.equal(result.codes[0], code)
  assert.ok(!result.html.includes('<script>'))
  assert.ok(result.html.includes('&lt;script&gt;'))
})

test('资源路径替换与表格滚动包装', () => {
  const result = renderDoc('![图](diagram.svg)\n\n|列|值|\n|---|---|\n|A|B|', { 'diagram.svg': '/assets/diagram.svg' })
  assert.ok(result.html.includes('src="/assets/diagram.svg"'))
  assert.ok(result.html.includes('overflow-x-auto'))
  assert.ok(result.html.includes('<table'))
})

test('两篇真实场景文档均生成完整且独立的目录', () => {
  const auth = renderDoc(readFileSync(new URL('../../../scenarios/01-auth/design.md', import.meta.url), 'utf8'))
  const flash = renderDoc(readFileSync(new URL('../../../scenarios/03-flash-sale/design.md', import.meta.url), 'utf8'))
  assert.equal(auth.headings.length, 10)
  assert.equal(flash.headings.length, 9)
  assert.equal(flash.codes.length, 2)
  assert.equal(auth.headings[0].id, 'section-1')
  assert.equal(flash.headings[0].id, 'section-1')
})
