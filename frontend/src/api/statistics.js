import api from './axios'

export const statisticsApi = {
  get() {
    return api.get('/statistics')
  },
  refreshSizes() {
    return api.post('/statistics/refresh-sizes')
  }
}
