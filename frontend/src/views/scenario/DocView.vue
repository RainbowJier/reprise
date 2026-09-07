<script setup>
import { computed } from 'vue'
import { marked } from 'marked'

import { scenarioDocs } from '@/config/scenarioDocs'

const props = defineProps({
  scenarioId: { type: String, required: true },
})

const doc = computed(() => scenarioDocs[props.scenarioId])

const html = computed(() => {
  if (!doc.value) {
    return '<p>该场景暂无技术文档。</p>'
  }
  let md = doc.value.markdown
  // md 内相对图片路径重写为 Vite 资源 URL（内容为仓库自有文件，可信源）
  for (const [rel, url] of Object.entries(doc.value.assets)) {
    md = md.replaceAll(`](${rel})`, `](${url})`)
  }
  return marked.parse(md)
})
</script>

<template>
  <section class="animate-fade-up rounded-card border border-line bg-white px-8 py-6">
    <article class="prose prose-sm max-w-none prose-img:mx-auto prose-img:rounded" v-html="html" />
  </section>
</template>
