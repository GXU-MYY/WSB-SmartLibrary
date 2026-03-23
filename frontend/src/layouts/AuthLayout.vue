<script setup lang="ts">
import AppLogo from '@/components/AppLogo.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
</script>

<template>
  <div class="auth-layout">
    <section class="auth-layout__shell surface-card">
      <div class="auth-layout__theme">
        <ThemeToggle compact icon-only />
      </div>

      <aside class="auth-layout__brand">
        <div class="auth-layout__brand-copy">
          <AppLogo compact />
          <span class="eyebrow">SmartLibrary</span>
          <h1>进入书库</h1>
          <p>登录或注册后继续。</p>
        </div>

        <div class="auth-layout__visual" aria-hidden="true">
          <div class="auth-layout__card auth-layout__card--primary">
            <span>Library Card</span>
            <strong>WSB</strong>
            <small>Volume 01</small>
          </div>

          <div class="auth-layout__card auth-layout__card--secondary">
            <span>Reading Room</span>
            <strong>Paper Desk</strong>
          </div>

          <div class="auth-layout__shelf">
            <span />
            <span />
            <span />
            <span />
            <span />
          </div>
        </div>
      </aside>

      <main class="auth-layout__panel">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-layout__shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(320px, 0.4fr) minmax(0, 0.6fr);
  width: min(1120px, calc(100vw - 48px));
  min-height: min(760px, calc(100vh - 48px));
  overflow: hidden;
  background: var(--sl-paper-strong);
}

.auth-layout__theme {
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 2;
}

.auth-layout__brand {
  display: grid;
  align-content: space-between;
  gap: 22px;
  padding: 36px 28px 24px 24px;
  background:
    linear-gradient(180deg, rgba(244, 237, 224, 0.95), rgba(238, 228, 210, 0.9)),
    var(--sl-paper-muted);
  border-right: 1px solid var(--sl-line);
}

.auth-layout__brand-copy {
  display: grid;
  justify-items: start;
  gap: 14px;
}

.auth-layout__brand-copy h1,
.auth-layout__brand-copy p {
  margin: 0;
}

.auth-layout__brand-copy h1 {
  font-size: clamp(3rem, 5vw, 4.7rem);
  line-height: 0.92;
  letter-spacing: -0.05em;
  white-space: nowrap;
}

.auth-layout__brand-copy p {
  color: var(--sl-ink-soft);
  font-size: 1rem;
}

.auth-layout__visual {
  position: relative;
  display: grid;
  align-content: end;
  gap: 18px;
  min-height: 300px;
}

.auth-layout__card {
  display: grid;
  gap: 6px;
  max-width: 250px;
  padding: 18px 18px 16px;
  border-radius: 24px;
  border: 1px solid rgba(34, 48, 67, 0.08);
  box-shadow: 0 24px 40px rgba(27, 39, 52, 0.08);
}

.auth-layout__card span,
.auth-layout__card small {
  color: var(--sl-ink-soft);
}

.auth-layout__card strong {
  font-family: 'Newsreader', 'STSong', serif;
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 0.9;
}

.auth-layout__card--primary {
  transform: rotate(-4deg);
  background: rgba(255, 252, 247, 0.95);
}

.auth-layout__card--secondary {
  justify-self: end;
  transform: rotate(7deg) translateY(-18px);
  background: rgba(215, 231, 239, 0.92);
}

.auth-layout__card--secondary strong {
  font-size: clamp(1.9rem, 3.6vw, 2.8rem);
}

.auth-layout__shelf {
  display: flex;
  align-items: end;
  gap: 12px;
  margin-top: -28px;
  padding-left: 8px;
}

.auth-layout__shelf span {
  width: 48px;
  border-radius: 16px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.14);
}

.auth-layout__shelf span:nth-child(1) {
  height: 132px;
  background: linear-gradient(180deg, #2b6c79, #275c67);
}

.auth-layout__shelf span:nth-child(2) {
  height: 168px;
  background: linear-gradient(180deg, #d99146, #c9772e);
}

.auth-layout__shelf span:nth-child(3) {
  height: 184px;
  background: linear-gradient(180deg, #477c87, #386976);
}

.auth-layout__shelf span:nth-child(4) {
  height: 156px;
  background: linear-gradient(180deg, #e3aa63, #d2924b);
}

.auth-layout__shelf span:nth-child(5) {
  height: 142px;
  background: linear-gradient(180deg, #d6c7aa, #c7b694);
}

.auth-layout__panel {
  display: grid;
  align-items: center;
  padding: 48px 44px 40px;
}

@media (max-width: 900px) {
  .auth-layout {
    padding: 12px;
  }

  .auth-layout__shell {
    grid-template-columns: 1fr;
    width: min(100%, 760px);
    min-height: auto;
  }

  .auth-layout__theme {
    top: 14px;
    right: 14px;
  }

  .auth-layout__brand {
    gap: 14px;
    padding: 22px 18px 14px;
    border-right: 0;
    border-bottom: 1px solid var(--sl-line);
  }

  .auth-layout__brand-copy {
    gap: 10px;
    max-width: calc(100% - 60px);
  }

  .auth-layout__brand-copy h1 {
    font-size: clamp(2.2rem, 8vw, 3rem);
  }

  .auth-layout__brand-copy p {
    font-size: 0.94rem;
  }

  .auth-layout__visual {
    min-height: auto;
    gap: 10px;
  }

  .auth-layout__card {
    display: none;
  }

  .auth-layout__shelf {
    gap: 10px;
    margin-top: 0;
    padding-left: 0;
  }

  .auth-layout__shelf span:nth-child(3),
  .auth-layout__shelf span:nth-child(4),
  .auth-layout__shelf span:nth-child(5) {
    display: none;
  }

  .auth-layout__shelf span:nth-child(1) {
    height: 54px;
    width: 22px;
    border-radius: 10px;
  }

  .auth-layout__shelf span:nth-child(2) {
    height: 66px;
    width: 22px;
    border-radius: 10px;
  }

  .auth-layout__panel {
    padding: 22px 18px 20px;
  }
}
</style>
