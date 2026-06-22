import api from './axios'

export const downloadsApi = {
  seedGame(id) {
    return api.post(`/games/${id}/seed`)
  },
  getSeedStatus(taskId) {
    return api.get(`/seed/status/${taskId}`)
  },
  getActive() {
    return api.get('/downloads/active')
  },
  getWaiting(offset = 0, num = 50) {
    return api.get('/downloads/waiting', { params: { offset, num } })
  },
  getStopped(offset = 0, num = 50) {
    return api.get('/downloads/stopped', { params: { offset, num } })
  },
  getStatus(gid) {
    return api.get(`/downloads/${gid}/status`)
  },
  remove(gid) {
    return api.post(`/downloads/${gid}/remove`)
  },
  pause(gid) {
    return api.post(`/downloads/${gid}/pause`)
  },
  unpause(gid) {
    return api.post(`/downloads/${gid}/unpause`)
  },
  getGlobalStat() {
    return api.get('/downloads/global-stat')
  },
  getAria2Version() {
    return api.get('/downloads/aria2-version')
  },
  getDownloadUrl(id) {
    return `/game-library/api/games/${id}/download`
  }
}
