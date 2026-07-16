import api from './axios'

export const statisticsApi = {
  get() {
    return api.get('/statistics')
  }
}
