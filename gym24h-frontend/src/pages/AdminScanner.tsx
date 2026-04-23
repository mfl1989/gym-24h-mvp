import { Html5Qrcode } from 'html5-qrcode'
import { isAxiosError } from 'axios'
import { useEffect, useRef, useState } from 'react'
import api from '../api/axios'

type ScanState = 'idle' | 'scanning' | 'success' | 'error'

type ApiResponse<T> = {
  success: boolean
  data: T
}

type AuthTokenPayload = {
  authToken: string
  expiresAt: string
}

type EntranceTokenPayload = {
  token: string
}

const SCANNER_REGION_ID = 'admin-scanner-region'
const ADMIN_TOKEN_STORAGE_KEY = 'adminScannerToken'
const SCAN_ENDPOINT = '/admin/entrances/scan'
const LOCAL_ADMIN_TOKEN = 'super-secret-admin-key'
const DEV_LOGIN_USER_ID = '11111111-1111-1111-1111-111111111111'

function isLocalDevelopment() {
  return window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
}

export default function AdminScanner() {
  const scannerRef = useRef<Html5Qrcode | null>(null)
  const isHandlingResultRef = useRef(false)
  const adminTokenRef = useRef('')
  const [adminToken, setAdminToken] = useState(() => localStorage.getItem(ADMIN_TOKEN_STORAGE_KEY) ?? '')
  const [manualToken, setManualToken] = useState('')
  const [scanState, setScanState] = useState<ScanState>('idle')
  const [statusMessage, setStatusMessage] = useState('等待核销。可使用摄像头自动扫码，或直接粘贴 Token 手动核销。')
  const [lastTokenPreview, setLastTokenPreview] = useState('')
  const [cameraMessage, setCameraMessage] = useState('正在初始化摄像头扫描器...')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isGeneratingTestToken, setIsGeneratingTestToken] = useState(false)
  const localDevelopment = isLocalDevelopment()

  useEffect(() => {
    localStorage.setItem(ADMIN_TOKEN_STORAGE_KEY, adminToken)
    adminTokenRef.current = adminToken
  }, [adminToken])

  useEffect(() => {
    if (!localDevelopment) {
      return
    }

    setAdminToken((currentToken) => currentToken || LOCAL_ADMIN_TOKEN)
  }, [localDevelopment])

  async function verifyToken(rawToken: string) {
    const qrToken = rawToken.trim()

    if (isHandlingResultRef.current) {
      return
    }

    if (!adminTokenRef.current.trim()) {
      setScanState('error')
      setStatusMessage('请先输入管理员令牌，再执行核销。')
      return
    }

    if (!qrToken) {
      setScanState('error')
      setStatusMessage('请提供有效的二维码 Token。')
      return
    }

    isHandlingResultRef.current = true
    setIsSubmitting(true)
    setLastTokenPreview(qrToken.slice(0, 18))

    try {
      await api.post<ApiResponse<string>>(
        SCAN_ENDPOINT,
        { qrToken },
        {
          headers: {
            'X-Admin-Token': adminTokenRef.current.trim(),
          },
        },
      )

      setScanState('success')
      setStatusMessage('准予进入，门锁开门请求已受理。')
      setManualToken('')
    } catch (error) {
      const message = isAxiosError(error)
        ? (error.response?.data as { message?: string } | undefined)?.message ?? '核销失败，已拒绝通行。'
        : '核销失败，已拒绝通行。'
      setScanState('error')
      setStatusMessage(message)
    } finally {
      setIsSubmitting(false)
      window.setTimeout(() => {
        isHandlingResultRef.current = false
      }, 1200)
    }
  }

  async function verifyLocalTestMember() {
    if (!localDevelopment) {
      return
    }

    setIsGeneratingTestToken(true)
    setStatusMessage('正在生成本地测试会员 Token 并自动核销...')

    try {
      const loginResponse = await api.post<ApiResponse<AuthTokenPayload>>('/auth/dev-login', {
        userId: DEV_LOGIN_USER_ID,
      })
      const authToken = loginResponse.data.data.authToken
      localStorage.setItem('authToken', authToken)

      const tokenResponse = await api.get<ApiResponse<EntranceTokenPayload>>('/me/entrance-token', {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      })

      const qrToken = tokenResponse.data.data.token
      setManualToken(qrToken)
      await verifyToken(qrToken)
    } catch (error) {
      const message = isAxiosError(error)
        ? (error.response?.data as { message?: string } | undefined)?.message ?? '本地测试核销失败，请检查后端是否已启动。'
        : '本地测试核销失败，请检查后端是否已启动。'
      setScanState('error')
      setStatusMessage(message)
    } finally {
      setIsGeneratingTestToken(false)
    }
  }

  useEffect(() => {
    let isCancelled = false

    async function startScanner() {
      const scanner = new Html5Qrcode(SCANNER_REGION_ID)
      scannerRef.current = scanner

      try {
        setScanState('scanning')
        setCameraMessage('摄像头已连接，请将会员二维码对准扫描框。')

        await scanner.start(
          { facingMode: 'environment' },
          {
            fps: 10,
            qrbox: { width: 240, height: 240 },
            aspectRatio: 1,
          },
          async (decodedText) => {
            if (isCancelled || isHandlingResultRef.current) {
              return
            }

            await verifyToken(decodedText)
          },
          () => {
            // ignore
          },
        )
      } catch {
        if (!isCancelled) {
          setCameraMessage('摄像头未就绪，请检查设备权限，或直接使用下方手动核销入口。')
        }
      }
    }

    void startScanner()

    return () => {
      isCancelled = true
      const scanner = scannerRef.current
      scannerRef.current = null

      if (scanner) {
        void (async () => {
          try {
            await scanner.stop()
          } catch (error) {
            const message = error instanceof Error ? error.message : String(error)
            if (!message.includes('scanner is not running or paused')) {
              console.warn('Failed to stop scanner during cleanup:', error)
            }
          }

          try {
            scanner.clear()
          } catch (error) {
            const message = error instanceof Error ? error.message : String(error)
            if (!message.includes('scanner is not running or paused')) {
              console.warn('Failed to clear scanner during cleanup:', error)
            }
          }
        })()
      }
    }
  }, [])

  const accentClassName = scanState === 'success'
    ? 'border-emerald-400/70 bg-emerald-500/15 text-emerald-100 shadow-[0_0_0_1px_rgba(74,222,128,0.16),0_24px_60px_rgba(22,163,74,0.26)]'
    : scanState === 'error'
      ? 'border-rose-400/70 bg-rose-500/15 text-rose-100 shadow-[0_0_0_1px_rgba(251,113,133,0.16),0_24px_60px_rgba(225,29,72,0.24)]'
      : 'border-cyan-400/40 bg-cyan-500/10 text-cyan-50 shadow-[0_0_0_1px_rgba(34,211,238,0.16),0_24px_60px_rgba(8,145,178,0.18)]'

  const panelClassName = scanState === 'success'
    ? 'border-emerald-400/30 bg-[radial-gradient(circle_at_top,_rgba(16,185,129,0.2),_transparent_28%),linear-gradient(180deg,_#03140d_0%,_#052e16_100%)]'
    : scanState === 'error'
      ? 'border-rose-400/30 bg-[radial-gradient(circle_at_top,_rgba(244,63,94,0.18),_transparent_28%),linear-gradient(180deg,_#18030a_0%,_#3b0912_100%)]'
      : 'border-white/10 bg-slate-950/85'

  return (
    <section className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(34,211,238,0.18),_transparent_28%),radial-gradient(circle_at_bottom,_rgba(16,185,129,0.16),_transparent_24%),linear-gradient(180deg,_#020617_0%,_#0f172a_100%)] px-6 py-8 text-white">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-6 lg:flex-row">
        <div className={`flex-1 overflow-hidden rounded-[2rem] border p-5 shadow-[0_24px_80px_rgba(15,23,42,0.45)] backdrop-blur-xl transition ${panelClassName}`}>
          <div className="mb-5 flex items-center justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-[0.36em] text-cyan-300/80">Admin Scanner</p>
              <h1 className="mt-3 text-3xl font-semibold tracking-tight text-white">入馆核销工作台</h1>
            </div>
            <div className={`rounded-full px-4 py-2 text-xs font-semibold ${accentClassName}`}>
              {scanState === 'success' ? '准予进入' : scanState === 'error' ? '拒绝通行' : '等待核销'}
            </div>
          </div>

          <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-black/40 p-3 shadow-[inset_0_1px_0_rgba(255,255,255,0.06)]">
            <div id={SCANNER_REGION_ID} className="min-h-[420px] rounded-[1.35rem] bg-slate-900/80" />
          </div>

          <div className="mt-4 rounded-[1.5rem] border border-white/10 bg-white/5 px-4 py-3 text-sm leading-6 text-slate-300">
            {cameraMessage}
          </div>

          <div className="mt-4 rounded-[1.75rem] border border-white/10 bg-white/6 p-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.04)]">
            <div className="flex flex-col gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.28em] text-amber-300/80">Manual Verify</p>
                <p className="mt-2 text-sm text-slate-300">没有摄像头时，直接粘贴会员二维码 Token 进行强制核销。</p>
              </div>

              {localDevelopment ? (
                <button
                  type="button"
                  onClick={() => void verifyLocalTestMember()}
                  disabled={isSubmitting || isGeneratingTestToken}
                  className="inline-flex items-center justify-center rounded-2xl bg-[linear-gradient(135deg,_#22c55e_0%,_#14b8a6_100%)] px-4 py-3 text-sm font-semibold text-slate-950 shadow-[0_18px_40px_rgba(20,184,166,0.24)] transition hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isGeneratingTestToken ? '测试核销中...' : '本地测试会员一键核销'}
                </button>
              ) : null}

              <input
                type="text"
                value={manualToken}
                onChange={(event) => setManualToken(event.target.value)}
                placeholder="粘贴二维码 Token"
                className="w-full rounded-2xl border border-white/10 bg-slate-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-amber-300/60 focus:ring-2 focus:ring-amber-300/15"
              />

              <button
                type="button"
                onClick={() => void verifyToken(manualToken)}
                disabled={isSubmitting || isGeneratingTestToken}
                className="inline-flex items-center justify-center rounded-2xl bg-[linear-gradient(135deg,_#f59e0b_0%,_#f97316_100%)] px-4 py-3 text-sm font-semibold text-slate-950 shadow-[0_18px_40px_rgba(249,115,22,0.28)] transition hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting ? '核销中...' : '强制核销 (Manual Verify)'}
              </button>
            </div>
          </div>
        </div>

        <aside className="w-full max-w-xl rounded-[2rem] border border-white/10 bg-white/10 p-6 shadow-[0_24px_80px_rgba(15,23,42,0.35)] backdrop-blur-xl lg:max-w-sm">
          <div className="space-y-5">
            <div>
              <p className="text-xs uppercase tracking-[0.32em] text-emerald-300/80">Control</p>
              <h2 className="mt-3 text-2xl font-semibold tracking-tight text-white">现场控制参数</h2>
            </div>

            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-200">管理员令牌</span>
              <input
                type="password"
                value={adminToken}
                onChange={(event) => setAdminToken(event.target.value)}
                placeholder={localDevelopment ? '本地环境已自动填入管理员令牌' : '输入 X-Admin-Token'}
                className="w-full rounded-2xl border border-white/10 bg-slate-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-cyan-400/60 focus:ring-2 focus:ring-cyan-400/20"
              />
            </label>

            <div className={`rounded-[1.5rem] border px-4 py-4 text-sm leading-6 ${accentClassName}`}>
              {statusMessage}
            </div>

            <div className="rounded-[1.5rem] border border-white/10 bg-slate-950/55 px-4 py-4 text-sm text-slate-300">
              <p className="font-medium text-slate-100">最近一次核销</p>
              <p className="mt-2 break-all text-xs text-slate-400">
                {lastTokenPreview ? `${lastTokenPreview}...` : '尚未处理任何 Token'}
              </p>
            </div>

            <div className="rounded-[1.5rem] border border-white/10 bg-white/5 px-4 py-4 text-sm leading-6 text-slate-300">
              同一条核心链路会同时服务于摄像头扫码和手动粘贴核销。绿屏表示准予进入，红屏表示拒绝通行并附带后端原因。
            </div>
          </div>
        </aside>
      </div>
    </section>
  )
}