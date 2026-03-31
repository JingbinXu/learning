
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /api/ */
export async function healthCheck(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/api/', {
    method: 'GET',
    ...(options || {}),
  })
}
