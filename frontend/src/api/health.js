import api from './axios'

export const healthApi = {
  getReport() {
    return api.get('/library/health')
  },
  getIssues(issueType, offset = 0, limit = 20) {
    return api.get(`/library/health/issues/${issueType}`, { params: { offset, limit } })
  },
  fixIssueType(issueType) {
    return api.post(`/library/health/fix/${issueType}`)
  },
  fixSingleGame(issueType, gameId) {
    return api.post(`/library/health/fix/${issueType}/${gameId}`)
  }
}
