import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPersist from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'
import 'element-plus/dist/index.css'
import './styles/global.scss'

const app = createApp(App)
const pinia = createPinia().use(piniaPersist)
app.use(pinia)
app.use(router)
app.mount('#app')
