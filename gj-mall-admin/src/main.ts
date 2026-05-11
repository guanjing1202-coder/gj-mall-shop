import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPersist from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/global.css'

const app = createApp(App)
app.use(createPinia().use(piniaPersist))
app.use(router)
app.use(Antd)
app.mount('#app')
