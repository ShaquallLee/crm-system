import request from '@/utils/request'

export function getCustomers(params) {
  return request.get('/customers', { params })
}

export function getCustomer(id) {
  return request.get(`/customers/${id}`)
}

export function createCustomer(data) {
  return request.post('/customers', data)
}

export function updateCustomer(data) {
  return request.put('/customers', data)
}

export function deleteCustomer(id) {
  return request.delete(`/customers/${id}`)
}

export function exportCustomers(params) {
  return request.get('/customers/export', { params, responseType: 'blob' })
}

export function downloadImportTemplate() {
  return request.get('/customers/import-template', { responseType: 'blob' })
}

export function importCustomers(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/customers/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
