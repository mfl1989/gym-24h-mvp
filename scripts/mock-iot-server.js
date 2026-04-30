const http = require('http')

const PORT = 8081

function writeJson(response, statusCode, payload) {
  response.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
  })
  response.end(JSON.stringify(payload))
}

function renderUnlockBanner(deviceId) {
  const label = `[ ${deviceId} UNLOCKED ! ]`
  const border = '='.repeat(label.length)
  const greenBgWhiteText = '\x1b[42m\x1b[97m'
  const reset = '\x1b[0m'

  console.log('')
  console.log(`${greenBgWhiteText}${border}${reset}`)
  console.log(`${greenBgWhiteText}${label}${reset}`)
  console.log(`${greenBgWhiteText}${border}${reset}`)
  console.log('')
}

const server = http.createServer((request, response) => {
  if (request.method !== 'POST' || request.url !== '/mock/door-lock/unlock') {
    writeJson(response, 404, { success: false, message: 'Not Found' })
    return
  }

  let body = ''
  request.on('data', (chunk) => {
    body += chunk
  })

  request.on('end', () => {
    let payload
    try {
      payload = body ? JSON.parse(body) : {}
    } catch (error) {
      writeJson(response, 400, { success: false, message: 'Invalid JSON payload' })
      return
    }

    const deviceId = typeof payload.deviceId === 'string' && payload.deviceId.trim()
      ? payload.deviceId.trim()
      : null

    if (!deviceId) {
      writeJson(response, 400, { success: false, message: 'deviceId is required' })
      return
    }

    renderUnlockBanner(deviceId)

    const delayMilliseconds = 500 + Math.floor(Math.random() * 301)
    setTimeout(() => {
      writeJson(response, 200, {
        success: true,
        deviceId,
        command: 'UNLOCK',
        delayedMs: delayMilliseconds,
      })
    }, delayMilliseconds)
  })
})

server.listen(PORT, () => {
  console.log(`[mock-iot] listening on http://localhost:${PORT}/mock/door-lock/unlock`)
})

server.on('error', (error) => {
  console.error('[mock-iot] server error:', error)
  process.exit(1)
})