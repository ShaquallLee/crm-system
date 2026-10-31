import request from '@/utils/request'

export function getOperationLogs(params) {
  return request.get('/audit/operations', { params })
}

export function getLoginLogs(params) {
  return request.get('/audit/logins', { params })
}
