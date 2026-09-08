<script setup>
defineProps({ title: String, subtitle: String, eyebrow: String })
</script>

<template>
  <main class="flex min-h-dvh items-center justify-center bg-canvas px-5 py-8 sm:py-12">
    <div class="grid w-full max-w-5xl overflow-hidden rounded-2xl border border-line bg-paper md:grid-cols-2">
      <aside class="relative hidden flex-col justify-between bg-primary px-10 py-10 text-white md:flex">
        <RouterLink to="/" class="flex items-center gap-3 text-xl font-semibold"><span class="flex h-9 w-9 items-center justify-center rounded-xl border border-white/20 font-mono">r.</span>reprise</RouterLink>
        <div class="py-16"><p class="mb-6 font-mono text-[10px] tracking-[0.2em] text-white/60">THE ENGINEERING FIELD NOTES</p><h2 class="font-display text-4xl leading-relaxed">把每一次实现，<br>变成下一次理解。</h2><p class="mt-6 max-w-xs text-sm leading-7 text-white/70">一份持续生长的工程实践记录。<br>读懂设计取舍，亲手运行真实场景。</p><div class="mt-10 flex gap-2"><span v-for="tag in ['技术文档', '交互演示', '设计取舍']" :key="tag" class="rounded-full border border-white/20 px-3 py-1 text-[10px] text-white/80">{{ tag }}</span></div></div>
        <p class="border-t border-white/15 pt-5 font-display text-lg italic text-white/60">Read. Build. Repeat.</p>
      </aside>
      <section class="flex flex-col justify-center p-7 sm:p-10 lg:p-12">
        <RouterLink to="/" class="mb-8 text-lg font-semibold text-primary md:hidden">r. reprise</RouterLink>
        <p class="mb-3 font-mono text-[10px] tracking-[0.18em] text-primary">{{ eyebrow }}</p><h1 class="text-2xl font-semibold tracking-tight">{{ title }}</h1><p class="mb-7 mt-3 text-sm leading-6 text-ink-secondary">{{ subtitle }}</p>
        <slot />
      </section>
    </div>
  </main>
</template>
