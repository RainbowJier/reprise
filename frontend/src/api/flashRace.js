// 并发演示专用裸 axios 实例：不走 http.js 拦截器——
// 洪峰虚拟用户各有独立 token，不能复用当前登录态的请求头注入与 401 续期逻辑
import axios from 'axios'

const raceHttp = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

/**
 * 注册一个洪峰虚拟用户（注册即登录，直接返回 token）。
 */
export async function registerRaceUser(username) {
  const { data: body } = await raceHttp.post('/auth/register', {
    username,
    password: 'pass123456',
  })
  if (body?.code !== 200) {
    throw new Error(body?.msg || `注册并发用户失败（code=${body?.code}）`)
  }
  return { username, accessToken: body.data.accessToken }
}

/**
 * 单次抢购请求。竞速结果本身就是数据：HTTP 层异常也不 reject，统一归入结果码。
 *
 * @returns {{code: number, msg: string, orderId: string|null, rt: number}}
 */
export async function raceSeckill(itemId, accessToken) {
  const started = performance.now()
  const rt = () => Math.round(performance.now() - started)
  try {
    const { data: body } = await raceHttp.post(
      `/flash/items/${itemId}/seckill`,
      {},
      { headers: { Authorization: `Bearer ${accessToken}` } },
    )
    return {
      code: body?.code ?? -1,
      msg: body?.msg ?? '',
      orderId: body?.data?.orderId ?? null,
      rt: rt(),
    }
  } catch (error) {
    return { code: -1, msg: error.message, orderId: null, rt: rt() }
  }
}
