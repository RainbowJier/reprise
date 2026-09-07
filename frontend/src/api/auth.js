import { post } from './http'

export const register = (data) => post('/auth/register', data)

export const login = (data) => post('/auth/login', data)
