import { Marked } from 'marked'

const escapeHtml = value => value.replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char])

// 文档只接受仓库内受信 Markdown；该渲染器不用于外部用户内容。
export function renderDoc(markdown, assets = {}) {
  let source = markdown
  for (const [rel, url] of Object.entries(assets)) source = source.replaceAll(`](${rel})`, `](${url})`)
  const headings = []
  const codes = []
  const parser = new Marked()
  parser.use({ renderer: {
    heading({ tokens, depth }) {
      const text = this.parser.parseInline(tokens)
      if (depth === 1) return ''
      const id = `section-${headings.length + 1}`
      if (depth <= 3) headings.push({ id, label: tokens.map(token => token.text || token.raw || '').join(''), depth })
      return `<h${depth} id="${id}" class="scroll-mt-28">${text}</h${depth}>`
    },
    code({ text, lang }) {
      const index = codes.push(text) - 1
      const language = escapeHtml((lang || 'text').split(/\s/)[0])
      return `<div class="not-prose my-6 overflow-hidden rounded-xl border border-line"><div class="flex items-center justify-between border-b border-line bg-code-bg px-4 py-2"><span class="font-mono text-[10px] uppercase tracking-widest text-ink-secondary">${language}</span><button type="button" data-copy-code="${index}" class="rounded px-2 py-1 text-[11px] text-primary hover:bg-primary-soft" aria-label="复制代码块 ${index + 1}">复制代码</button></div><pre class="m-0 overflow-x-auto bg-code-panel p-5 font-mono text-xs leading-6 text-code-text"><code>${escapeHtml(text)}</code></pre></div>`
    },
    table(token) {
      const cell = (value, tag) => `<${tag} class="border-b border-line px-4 py-3 text-left">${this.parser.parseInline(value.tokens)}</${tag}>`
      return `<div class="not-prose my-6 overflow-x-auto rounded-xl border border-line"><table class="w-full min-w-[480px] border-collapse text-xs leading-6"><thead class="bg-code-bg text-ink"><tr>${token.header.map(value => cell(value, 'th')).join('')}</tr></thead><tbody class="text-ink-secondary">${token.rows.map(row => `<tr>${row.map(value => cell(value, 'td')).join('')}</tr>`).join('')}</tbody></table></div>`
    },
  } })
  return { html: parser.parse(source), headings, codes }
}
