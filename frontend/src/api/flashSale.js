import { get, post } from './http'

export const listFlashItems = () => get('/flash/items')

export const seckillFlashItem = (itemId) => post(`/flash/items/${itemId}/seckill`)

export const listMyFlashOrders = () => get('/flash/orders/mine')

// 演示专用：重置商品库存与订单（仅进行中活动；使并发演示可反复运行）
export const resetFlashDemoItem = (itemId) => post(`/flash/demo/reset/${itemId}`)
