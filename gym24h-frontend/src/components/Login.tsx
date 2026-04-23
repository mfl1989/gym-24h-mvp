import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useLiff } from '../contexts/LiffContext'

function isLocalDevelopment() {
  return window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
}

export default function Login() {
  const navigate = useNavigate()
  const { isLoggedIn, isLoading } = useLiff()
  const localDevelopment = isLocalDevelopment()

  useEffect(() => {
    if (!isLoading && isLoggedIn) {
      navigate('/dashboard', { replace: true })
    }
  }, [isLoading, isLoggedIn, navigate])

  return (
    <section className="flex min-h-screen items-center justify-center px-6 py-10">
      <div className="w-full max-w-sm rounded-[2rem] border border-white/10 bg-white/95 p-6 shadow-[0_24px_80px_rgba(15,23,42,0.35)]">
        <div className="rounded-[1.5rem] bg-slate-950 px-5 py-6 text-white">
          <p className="text-xs uppercase tracking-[0.32em] text-emerald-300">Gym24h Access</p>
          <h1 className="mt-4 text-3xl font-semibold tracking-tight text-white">LINE 登录</h1>
          <p className="mt-3 text-sm leading-6 text-slate-300">
            {localDevelopment
              ? '当前是本地开发环境。系统会跳过 LIFF，直接使用开发账号自动登录并跳转到仪表盘。'
              : '请从 LINE 聊天或图文菜单中打开此页面。进入 LIFF 后会自动完成身份验证并跳转到仪表盘。'}
          </p>
        </div>

        <div className="mt-6 space-y-4">
          <div className="rounded-[1.25rem] border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm leading-6 text-emerald-800">
            {isLoading
              ? localDevelopment
                ? '正在使用本地开发账号自动登录...'
                : '正在连接 LINE 身份服务...'
              : localDevelopment
                ? '本地开发模式已启用。若尚未跳转，请刷新页面重新触发自动登录。'
                : '如果当前不在 LINE 内打开，LIFF 无法返回真实用户身份。'}
          </div>
        </div>
      </div>
    </section>
  )
}
