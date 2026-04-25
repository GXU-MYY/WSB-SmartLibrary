import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import router from './router'
import '@/assets/styles/main.css'
import { applyTheme, resolvePreferredTheme } from '@/utils/theme'

applyTheme(resolvePreferredTheme())

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
