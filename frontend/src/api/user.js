import { get } from './http'

export const getMe = () => get('/user/me')
