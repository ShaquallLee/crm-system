import request from '@/utils/request'

export function getContactsByCustomer(customerId) {
  return request.get(`/contacts/customer/${customerId}`)
}

export function createContact(data) {
  return request.post('/contacts', data)
}

export function updateContact(data) {
  return request.put('/contacts', data)
}

export function deleteContact(id) {
  return request.delete(`/contacts/${id}`)
}
