import request from "@/utils/request.js"

const MESSAGE_API_PREFIX = "/sms"

export const getMessages = params =>
  request({
    url: `${MESSAGE_API_PREFIX}/inboxes`,
    method: "get",
    params
  })

export const markMessageRead = id =>
  request({
    url: `${MESSAGE_API_PREFIX}/inboxes/${id}/read`,
    method: "put"
  })

export const markAllMessagesRead = () =>
  request({
    url: `${MESSAGE_API_PREFIX}/inboxes/read`,
    method: "put"
  })

export const deleteMessage = id =>
  request({
    url: `${MESSAGE_API_PREFIX}/inboxes/${id}`,
    method: "delete"
  })
