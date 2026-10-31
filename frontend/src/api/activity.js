import request from '@/utils/request'

export function getActivitiesByCustomer(customerId) {
  return request.get(`/activities/customer/${customerId}`)
}

export function getActivitiesByOpportunity(opportunityId) {
  return request.get(`/activities/opportunity/${opportunityId}`)
}

export function getTodoActivities() {
  return request.get('/activities/todo')
}

export function getReminders() {
  return request.get('/activities/reminders')
}

export function createActivity(data) {
  return request.post('/activities', data)
}

export function updateActivity(data) {
  return request.put('/activities', data)
}

export function deleteActivity(id) {
  return request.delete(`/activities/${id}`)
}
