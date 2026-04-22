import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

const DEV_USER_ID = '11111111-1111-1111-1111-111111111111'

type DevLoginResponse = {
  success: boolean
  data: {
    authToken: string
    expiresAt: string
  }
}

export default function Login() {
  const navigate = useNavigate()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleDevLogin() {
    try {
      setIsSubmitting(true)
      setErrorMessage(null)

      const response = await api.post<DevLoginResponse>('/auth/dev-login', {
        userId: DEV_USER_ID,
      })

      localStorage.setItem('authToken', response.data.data.authToken)
      navigate('/dashboard', { replace: true })
    } catch {
      setErrorMessage('开发者登录失败，请确认后端服务已启动且测试 userId 存在')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="flex min-h-screen items-center justify-center px-6 py-10">
      <div className="w-full max-w-sm rounded-[2rem] border border-white/10 bg-white/95 p-6 shadow-[0_24px_80px_rgba(15,23,42,0.35)]">
        <div className="rounded-[1.5rem] bg-slate-950 px-5 py-6 text-white">
          <p className="text-xs uppercase tracking-[0.32em] text-emerald-300">Gym24h Access</p>
          <h1 className="mt-4 text-3xl font-semibold tracking-tight text-white">开发环境登录</h1>
          <p className="mt-3 text-sm leading-6 text-slate-300">
            当前使用开发者快捷登录，后续这里将替换为真实 LINE LIFF 授权流程。
          </p>
        </div>

        <div className="mt-6 space-y-4">
          <button
            type="button"
            onClick={handleDevLogin}
            disabled={isSubmitting}
            className="flex w-full items-center justify-center rounded-[1.5rem] bg-emerald-500 px-5 py-5 text-base font-semibold text-slate-950 shadow-[0_16px_40px_rgba(16,185,129,0.35)] transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:bg-emerald-300"
          >
            {isSubmitting ? '登录中...' : 'LINE 快捷登录 (Dev 环境)'}
          </button>

          {errorMessage ? (
            <div className="rounded-[1.25rem] border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {errorMessage}
            </div>
          ) : null}
        </div>
      </div>
    </section>
  )
}
