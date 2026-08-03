import api from './axios'

export const healthApi = {
  getReport() {
    return api.get('/library/health')
  },
  getIssues(issueType, offset = 0, limit = 20) {
    return api.get(`/library/health/issues/${issueType}`, { params: { offset, limit } })
  }
}
