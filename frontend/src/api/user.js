import request from '@/utils/request'

export function getUsers(params) {
  return request.get('/users', { params })
}

export function getUser(id) {
  return request.get(`/users/${id}`)
}

export function getRoles() {
  return request.get('/users/roles')
}

export function getOwnerOptions() {
  return request.get('/users/options')
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(data) {
  return request.put('/users', data)
}

export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}

export function resetPassword(id, password) {
  return request.put(`/users/${id}/reset-password`, { password })
}
