import axios from 'axios'

const CODE_SUCCESS = 200

export const http = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== CODE_SUCCESS) {
      return Promise.reject(new Error(body.msg || `请求失败（code=${body.code}）`))
    }
    return response
  },
  (error) => {
    const msg = error.response?.data?.msg ?? error.message ?? '网络错误'
    return Promise.reject(new Error(msg))
  },
)

export async function get(url, config) {
  const { data } = await http.get(url, config)
  return data.data
}

export async function post(url, body, config) {
  const { data } = await http.post(url, body, config)
  return data.data
}
