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
  assert.equal(auth.headings.filter(heading => heading.depth === 2).length, 10)
  assert.ok(auth.headings.some(heading => heading.depth === 3 && heading.label.includes('按时间顺序')))
  assert.ok(auth.headings.some(heading => heading.depth === 3 && heading.label.includes('单飞')))
  assert.ok(auth.codes.some(code => code.includes('refreshInFlight')))
  assert.ok(auth.codes.some(code => code.includes('requestConfig.retried')))
  assert.equal(flash.headings.filter(heading => heading.depth === 2).length, 9)
  assert.ok(flash.headings.some(heading => heading.depth === 3 && heading.label.includes('执行顺序')))
  assert.ok(flash.headings.some(heading => heading.depth === 3 && heading.label.includes('同一事务')))
  assert.ok(flash.headings.some(heading => heading.depth === 3 && heading.label.includes('安全并发')))
  assert.ok(flash.codes.some(code => code.includes('AND stock > 0') && code.includes('AND deleted = 0')))
  assert.ok(flash.codes.some(code => code.includes('UNIQUE (item_id, user_id)')))
  assert.ok(flash.codes.some(code => code.includes('ROLLBACK')))
  assert.ok(flash.codes.some(code => code.includes('"mine": false')))
  assert.equal(auth.headings[0].id, 'section-1')
  assert.equal(flash.headings[0].id, 'section-1')
})

for (const scenario of [
  { id: '01-auth', name: '认证', assetDir: 'auth-jwt', chapters: 10 },
  { id: '03-flash-sale', name: '秒杀', assetDir: 'diagrams', chapters: 9 },
]) {
  test(`${scenario.name}讲义每个主章节都有可解析并已登记的 SVG 配图`, () => {
    const docUrl = new URL(`../../../scenarios/${scenario.id}/design.md`, import.meta.url)
    const markdown = readFileSync(docUrl, 'utf8')
    const registryUrl = new URL('../config/scenarioDocs.js', import.meta.url)
    const registry = readFileSync(registryUrl, 'utf8')
    const imports = new Map([...registry.matchAll(/import\s+(\w+)\s+from\s+'([^']+\.svg)\?url'/g)].map(match => [match[1], match[2]]))
    const mappings = new Map([...registry.matchAll(/'([^']+\.svg)'\s*:\s*(\w+)/g)].map(match => [match[1], match[2]]))
    const chapters = markdown.split(/^## /m).slice(1)
    const assets = {}
    assert.equal(chapters.length, scenario.chapters)
    chapters.forEach((chapter, index) => {
      const images = [...chapter.matchAll(/!\[([^\]]+)\]\(([^)]+\.svg)\)/g)]
      assert.equal(images.length, 1, `第 ${index + 1} 章应有一张主图`)
      const [, alt, relativePath] = images[0]
      assert.ok(alt.startsWith(`图${index + 1}：`))
      assert.ok(relativePath.startsWith(`${scenario.assetDir}/`))
      const svg = readFileSync(new URL(relativePath, docUrl), 'utf8')
      assert.ok(svg.includes('<svg '))
      assert.ok(svg.includes('<title id="figure-title">'))
      assert.ok(svg.includes('<desc id="figure-desc">'))
      assert.ok(svg.includes(`图 ${index + 1}｜`))
      assert.ok(!/<(?:script|image|foreignObject)\b/i.test(svg))
      const importedPath = imports.get(mappings.get(relativePath))
      assert.ok(importedPath, `${relativePath} 缺少资源登记`)
      const importedSvg = readFileSync(new URL(importedPath, registryUrl), 'utf8')
      assert.equal(importedSvg, svg)
      assets[relativePath] = `/assets/${scenario.id}-figure-${index + 1}.svg`
    })
    assert.equal(new Set(Object.keys(assets)).size, scenario.chapters)
    const html = renderDoc(markdown, assets).html
    assert.equal((html.match(/<img /g) || []).length, scenario.chapters)
    assert.ok(!html.includes(`src="${scenario.assetDir}/`))
  })
}
