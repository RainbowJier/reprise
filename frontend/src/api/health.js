import { get } from './http'

export const getHealth = () => get('/health')
