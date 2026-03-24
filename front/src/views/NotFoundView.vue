<template>
  <div class="page-shell page-stack not-found">
    <section class="surface-card not-found__card">
      <span class="eyebrow">404</span>
      <h1>这一页没有被放进书架。</h1>
      <p>可能是地址写错了，也可能是这条路径还没有被整理出来。</p>
      <RouterLink class="button button--primary" to="/home">回到首页</RouterLink>
    </section>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: grid;
  place-items: center;
}

.not-found__card {
  display: grid;
  gap: 14px;
  max-width: 560px;
  padding: 28px;
}

.not-found__card h1,
.not-found__card p {
  margin: 0;
}

.not-found__card h1 {
  font-size: clamp(2.6rem, 6vw, 4.8rem);
  line-height: 0.94;
}

.not-found__card p {
  color: var(--sl-ink-soft);
  line-height: 1.8;
}
</style>
