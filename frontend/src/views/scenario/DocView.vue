<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { scenarioDocs } from '@/config/scenarioDocs'
import { renderDoc } from '@/utils/renderDoc'

const props = defineProps({ scenarioId: { type: String, required: true } })
const route = useRoute()
const article = ref(null)
const activeHeading = ref('')
const progress = ref(0)
const copyMessage = ref('')
let feedbackTimer
const doc = computed(() => scenarioDocs[props.scenarioId])
const rendered = computed(() => doc.value ? renderDoc(doc.value.markdown, doc.value.assets) : { html: '', headings: [], codes: [] })
const readingMinutes = computed(() => Math.max(1, Math.ceil((doc.value?.markdown.length || 0) / 700)))
let headingElements = []
let frame

const updateReading = () => {
  if (!article.value) return
  const bounds = article.value.getBoundingClientRect()
  const total = bounds.height - window.innerHeight + 120
  progress.value = Math.min(100, Math.max(0, Math.round((120 - bounds.top) / Math.max(1, total) * 100)))
  activeHeading.value = headingElements.filter(el => el.getBoundingClientRect().top <= 150).at(-1)?.id || headingElements[0]?.id || ''
}
const scheduleReading = () => { cancelAnimationFrame(frame); frame = requestAnimationFrame(updateReading) }
const resetDocument = async () => {
  await nextTick()
  headingElements = [...(article.value?.querySelectorAll('h2,h3') || [])]
  if (route.hash) document.getElementById(route.hash.slice(1))?.scrollIntoView()
  updateReading()
}
const copyCode = async event => {
  const button = event.target.closest('[data-copy-code]')
  if (!button) return
  const code = rendered.value.codes[Number(button.dataset.copyCode)]
  if (code === undefined) return
  try {
    if (!navigator.clipboard) throw new Error('Clipboard unavailable')
    await navigator.clipboard.writeText(code)
    copyMessage.value = '代码已复制到剪贴板'
  } catch {
    copyMessage.value = '浏览器未允许复制，请选中代码后手动复制'
  }
  clearTimeout(feedbackTimer)
  feedbackTimer = setTimeout(() => { copyMessage.value = '' }, 3000)
}
watch(() => props.scenarioId, resetDocument)
onMounted(() => {
  resetDocument()
  window.addEventListener('scroll', scheduleReading, { passive: true })
  window.addEventListener('resize', scheduleReading)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', scheduleReading)
  window.removeEventListener('resize', scheduleReading)
  cancelAnimationFrame(frame)
  clearTimeout(feedbackTimer)
})
</script>

<template>
  <section v-if="doc" class="animate-fade-up">
    <div class="mb-7 flex flex-wrap items-center gap-4 text-[11px] text-ink-secondary"><span class="flex items-center gap-1.5"><AppIcon name="book" class="h-3.5 w-3.5" />约 {{ readingMinutes }} 分钟阅读</span><span>{{ rendered.headings.length }} 个章节</span><span class="ml-auto font-mono">TECHNICAL NOTES</span></div>
    <details class="mb-6 rounded-lg border border-line bg-paper px-4 py-3 xl:hidden"><summary class="cursor-pointer text-xs font-medium text-primary">本文目录 · {{ rendered.headings.length }} 个章节</summary><nav class="mt-3 grid gap-2 text-xs sm:grid-cols-2" aria-label="移动端本文目录"><a v-for="h in rendered.headings" :key="h.id" :href="`#${h.id}`" class="py-1 text-ink-secondary hover:text-primary">{{ h.label }}</a></nav></details>
    <div class="grid items-start gap-9 xl:grid-cols-[minmax(0,1fr)_176px]">
      <article ref="article" aria-label="技术文档正文" class="prose prose-sm min-w-0 max-w-none break-words prose-headings:font-semibold prose-headings:tracking-tight prose-headings:text-ink prose-h2:mb-5 prose-h2:mt-10 prose-h2:border-t prose-h2:border-line prose-h2:pt-8 prose-h2:text-xl prose-h3:text-base prose-p:leading-7 prose-p:text-ink-secondary prose-a:font-medium prose-a:text-primary prose-a:decoration-primary/30 prose-a:underline-offset-4 prose-strong:text-ink prose-code:rounded prose-code:bg-code-bg prose-code:px-1 prose-code:py-0.5 prose-code:font-normal prose-code:text-primary prose-code:before:content-none prose-code:after:content-none prose-li:leading-7 prose-li:text-ink-secondary prose-blockquote:rounded-r-lg prose-blockquote:border-primary/40 prose-blockquote:bg-primary-soft/60 prose-blockquote:py-1 prose-blockquote:pr-4 prose-blockquote:not-italic prose-img:mx-auto prose-img:w-full prose-img:rounded-lg prose-img:border prose-img:border-line prose-img:bg-paper [&>h2:first-child]:mt-0 [&>h2:first-child]:border-0 [&>h2:first-child]:pt-0" v-html="rendered.html" @click="copyCode" />
      <aside class="sticky top-26 hidden max-h-[calc(100dvh-8rem)] overflow-y-auto xl:block"><div><p class="mb-4 text-[10px] font-semibold tracking-[0.15em] text-ink-secondary">本文目录 / CONTENTS</p><nav class="space-y-1 border-l border-line" aria-label="本文目录"><a v-for="h in rendered.headings" :key="h.id" :href="`#${h.id}`" :aria-current="activeHeading === h.id ? 'location' : undefined" class="-ml-px block border-l-2 py-1.5 pl-3 text-[11px] leading-5" :class="[activeHeading === h.id ? 'border-primary font-medium text-primary' : 'border-transparent text-ink-secondary hover:text-primary', h.depth === 3 ? 'pl-5' : '']">{{ h.label }}</a></nav><div class="mt-6 border-t border-line pt-4"><p class="flex justify-between text-[10px] text-ink-secondary"><span>阅读进度</span><span class="font-mono">{{ progress }}%</span></p><div class="mt-2 h-1 overflow-hidden rounded-full bg-line"><div class="h-full rounded-full bg-primary transition-[width]" :style="{ width: `${progress}%` }" /></div></div><a href="#main-content" class="mt-5 inline-block text-[11px] text-ink-secondary hover:text-primary">↑ 回到顶部</a></div></aside>
    </div>
    <p v-if="copyMessage" role="status" class="fixed bottom-6 left-1/2 z-40 w-max max-w-[90vw] -translate-x-1/2 rounded-xl bg-ink px-5 py-3 text-xs text-white shadow-lg">{{ copyMessage }}</p>
  </section>
  <div v-else class="rounded-xl border border-line bg-paper p-10 text-center text-sm text-ink-secondary">该场景暂无技术文档。</div>
</template>
