<script setup lang="ts">
import AppLogo from '@/components/AppLogo.vue'
</script>

<template>
  <div class="auth-layout">
    <section class="auth-layout__hero surface-card">
      <AppLogo />

      <div class="auth-layout__copy">
        <span class="eyebrow">Paper Workspace</span>
        <h1>把藏书、借阅、社交和 AI 阅读助手放到同一张工作台。</h1>
        <p>
          这不是传统后台，也不是单纯社区页。它更像一间可以整理、分享、分析和继续阅读的数字书房。
        </p>
      </div>

      <div class="auth-layout__shelf" aria-hidden="true">
        <span />
        <span />
        <span />
        <span />
      </div>

      <ul class="auth-layout__points list-reset">
        <li>个人书库：录书、上架、补全 ISBN 元数据</li>
        <li>流转管理：借入借出、阅读状态、回收提醒</li>
        <li>社区网络：群组分享、评论反馈、收藏热度</li>
        <li>智能辅助：相似推荐、AI 摘要、书评聚合</li>
      </ul>
    </section>

    <main class="auth-layout__form surface-card">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.15fr minmax(0, 520px);
  gap: 20px;
  padding: 18px;
}

.auth-layout__hero,
.auth-layout__form {
  padding: 28px;
}

.auth-layout__hero {
  display: grid;
  align-content: space-between;
  gap: 24px;
}

.auth-layout__copy {
  display: grid;
  gap: 14px;
}

.auth-layout__copy h1,
.auth-layout__copy p {
  margin: 0;
}

.auth-layout__copy h1 {
  font-size: clamp(2.8rem, 6vw, 5.6rem);
  line-height: 0.92;
  max-width: 9ch;
}

.auth-layout__copy p {
  max-width: 52ch;
  color: var(--sl-ink-soft);
  line-height: 1.8;
}

.auth-layout__shelf {
  display: flex;
  gap: 10px;
}

.auth-layout__shelf span {
  width: 56px;
  height: 160px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(31, 95, 107, 0.9), rgba(201, 119, 46, 0.82));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.auth-layout__shelf span:nth-child(2n) {
  background: linear-gradient(180deg, rgba(34, 48, 67, 0.88), rgba(31, 95, 107, 0.82));
}

.auth-layout__points {
  display: grid;
  gap: 12px;
  color: var(--sl-ink-soft);
}

.auth-layout__points li {
  position: relative;
  padding-left: 18px;
}

.auth-layout__points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.7em;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--sl-accent);
}

.auth-layout__form {
  display: grid;
  align-items: center;
}

@media (max-width: 1080px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .auth-layout__hero {
    min-height: auto;
  }
}
</style>
