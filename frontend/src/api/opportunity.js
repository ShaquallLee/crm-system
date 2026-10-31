import request from '@/utils/request'

export function getOpportunities(params) {
  return request.get('/opportunities', { params })
}

export function getOpportunity(id) {
  return request.get(`/opportunities/${id}`)
}

export function createOpportunity(data) {
  return request.post('/opportunities', data)
}

export function updateOpportunity(data) {
  return request.put('/opportunities', data)
}

export function deleteOpportunity(id) {
  return request.delete(`/opportunities/${id}`)
}
