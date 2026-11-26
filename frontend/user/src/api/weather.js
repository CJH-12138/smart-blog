import request from '@/utils/request'

// 获取实时天气
export function getWeather(params = {}) {
  return request({ 
    url: '/weather', 
    method: 'get',
    params,
    showError: false,
    timeout: 35000  // 35秒超时，比后端30秒稍长
  })
}
