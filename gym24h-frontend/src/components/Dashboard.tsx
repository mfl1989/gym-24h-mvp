import { QRCodeSVG } from 'qrcode.react'
import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import type { Invoice, MeProfileResponse, SubscriptionStatus } from '../types'
import InvoiceModal from './InvoiceModal'

type MeApiPayload = {
  displayName: string | null
  pictureUrl?: string | null
  membershipStatus: string
  subscriptionStatus: SubscriptionStatus
  subscriptionValidUntil?: string | null
  currentPeriodEndAt?: string | null
  cancelAtPeriodEnd?: boolean
  isCancelAtPeriodEnd?: boolean
}

type InvoiceApiPayload = {
  id?: string
  amountPaid?: number
  amount?: number | string
  status: string
  billedAt: string
}

type EntranceTokenPayload = {
  token: string
}

type ApiResponse<T> = {
  success: boolean
  data: T
}

type CheckoutSessionPayload = {
  checkoutUrl: string
}

const statusStyles: Record<string, string> = {
  ACTIVE: 'bg-emerald-500/15 text-emerald-300 ring-1 ring-emerald-400/30',
  ARREARS: 'bg-rose-500/15 text-rose-300 ring-1 ring-rose-400/30',
  CANCELED: 'bg-slate-500/15 text-slate-300 ring-1 ring-slate-400/30',
  EXPIRED: 'bg-amber-500/15 text-amber-300 ring-1 ring-amber-400/30',
  UNKNOWN: 'bg-slate-500/15 text-slate-300 ring-1 ring-slate-400/30',
}

function normalizeProfile(payload: MeApiPayload): MeProfileResponse {
  const currentPeriodEndAt = payload.currentPeriodEndAt ?? payload.subscriptionValidUntil ?? null

  return {
    displayName: payload.displayName,
    pictureUrl: payload.pictureUrl ?? null,
    membershipStatus: payload.membershipStatus,
    subscriptionStatus: payload.subscriptionStatus,
    subscriptionValidUntil: payload.subscriptionValidUntil ?? currentPeriodEndAt,
    currentPeriodEndAt,
    isCancelAtPeriodEnd: payload.isCancelAtPeriodEnd ?? payload.cancelAtPeriodEnd ?? false,
  }
}

function normalizeInvoice(payload: InvoiceApiPayload, index: number): Invoice {
  const resolvedAmount = typeof payload.amountPaid === 'number'
    ? payload.amountPaid
    : typeof payload.amount === 'number'
      ? payload.amount
      : typeof payload.amount === 'string'
        ? Math.round(Number(payload.amount.replace(/[^\d.-]/g, '')))
        : 0

  return {
    id: payload.id ?? `${payload.billedAt}-${payload.status}-${index}`,
    amountPaid: Number.isFinite(resolvedAmount) ? resolvedAmount : 0,
    status: payload.status,
    billedAt: payload.billedAt,
  }
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '未设置'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [profile, setProfile] = useState<MeProfileResponse | null>(null)
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [qrToken, setQrToken] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isInvoiceModalOpen, setIsInvoiceModalOpen] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  function handleUnauthorizedRedirect() {
    localStorage.removeItem('authToken')
    setProfile(null)
    setQrToken('')
    setErrorMessage(null)
    navigate('/', { replace: true })
  }

  async function fetchQrToken() {
    try {
      const response = await api.get<ApiResponse<EntranceTokenPayload>>('/me/entrance-token')
      setQrToken(response.data.data.token)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 401) {
        handleUnauthorizedRedirect()
        return
      }

      setErrorMessage('动态门禁码获取失败，请稍后重试')
      setQrToken('')
    }
  }

  async function fetchProfile() {
    try {
      setIsLoading(true)
      setErrorMessage(null)

      const response = await api.get<ApiResponse<MeApiPayload>>('/me')
      const nextProfile = normalizeProfile(response.data.data)
      setProfile(nextProfile)

      if (nextProfile.subscriptionStatus === 'ACTIVE') {
        await fetchQrToken()
      } else {
        setQrToken('')
      }
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 401) {
        handleUnauthorizedRedirect()
        return
      }

      setErrorMessage('仪表盘加载失败，请稍后重试')
      setProfile(null)
    } finally {
      setIsLoading(false)
    }
  }

  async function fetchInvoices() {
    try {
      setErrorMessage(null)
      const response = await api.get<ApiResponse<InvoiceApiPayload[]>>('/me/invoices')
      setInvoices(response.data.data.map((invoice, index) => normalizeInvoice(invoice, index)))
      setIsInvoiceModalOpen(true)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 401) {
        handleUnauthorizedRedirect()
        return
      }

      setErrorMessage('账单加载失败，请稍后重试')
    }
  }

  async function toggleCancel() {
    if (!profile) {
      return
    }

    try {
      setErrorMessage(null)

      if (profile.isCancelAtPeriodEnd) {
        await api.post('/me/subscription/revoke-cancel')
      } else {
        await api.post('/me/subscription/cancel')
      }

      await fetchProfile()
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 401) {
        handleUnauthorizedRedirect()
        return
      }

      setErrorMessage(profile.isCancelAtPeriodEnd ? '恢复续费失败，请稍后重试' : '申请退会失败，请稍后重试')
    }
  }

  async function createCheckoutSession() {
    try {
      setErrorMessage(null)
      const response = await api.post<ApiResponse<CheckoutSessionPayload>>('/api/payment/create-checkout-session')
      window.location.href = response.data.data.checkoutUrl
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 401) {
        handleUnauthorizedRedirect()
        return
      }

      setErrorMessage('创建支付会话失败，请稍后重试')
    }
  }

  useEffect(() => {
    void fetchProfile()
  }, [])

  if (isLoading) {
    return (
      <section className="mx-auto flex min-h-screen w-full max-w-md items-center justify-center px-5 py-10">
        <div className="w-full rounded-[2rem] border border-white/10 bg-white/10 p-8 text-center text-sm text-slate-200 shadow-2xl shadow-cyan-950/30 backdrop-blur-xl">
          正在加载会员信息...
        </div>
      </section>
    )
  }

  if (errorMessage && !profile) {
    return (
      <section className="mx-auto flex min-h-screen w-full max-w-md items-center justify-center px-5 py-10">
        <div className="w-full rounded-[2rem] border border-rose-400/20 bg-rose-500/10 p-8 text-center text-sm text-rose-100 shadow-2xl shadow-rose-950/20 backdrop-blur-xl">
          {errorMessage}
        </div>
      </section>
    )
  }

  if (!profile) {
    return (
      <section className="mx-auto flex min-h-screen w-full max-w-md items-center justify-center px-5 py-10">
        <div className="w-full rounded-[2rem] border border-white/10 bg-white/10 p-8 text-center text-sm text-slate-200 shadow-2xl shadow-cyan-950/30 backdrop-blur-xl">
          会员信息不存在
        </div>
      </section>
    )
  }

  const statusKey = profile.subscriptionStatus ?? 'UNKNOWN'
  const statusClassName = statusStyles[statusKey] ?? statusStyles.UNKNOWN
  const memberName = profile.displayName?.trim() || '会员'
  const pictureUrl = profile.pictureUrl?.trim() || null

  return (
    <>
      <section className="mx-auto min-h-screen w-full max-w-md px-5 py-6">
        <div className="overflow-hidden rounded-[2rem] border border-white/10 bg-white/95 shadow-[0_24px_80px_rgba(15,23,42,0.35)]">
          <div className="relative overflow-hidden bg-slate-950 px-6 pb-8 pt-7 text-white">
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,_rgba(34,211,238,0.26),_transparent_38%),radial-gradient(circle_at_top_left,_rgba(16,185,129,0.18),_transparent_30%)]" />
            <div className="relative space-y-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-4">
                  <div className="flex h-16 w-16 items-center justify-center overflow-hidden rounded-full border border-white/15 bg-[linear-gradient(145deg,_rgba(15,23,42,0.92),_rgba(8,47,73,0.92))] shadow-[0_18px_40px_rgba(6,182,212,0.18)] ring-1 ring-cyan-400/20 backdrop-blur-sm">
                    {pictureUrl ? (
                      <img
                        src={pictureUrl}
                        alt={`${memberName} avatar`}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center bg-[radial-gradient(circle_at_top,_rgba(34,211,238,0.28),_transparent_55%)] text-lg font-semibold tracking-[0.16em] text-cyan-100">
                        G24
                      </div>
                    )}
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-[0.28em] text-cyan-300/90">Gym24h Dashboard</p>
                    <h1 className="mt-3 text-3xl font-semibold tracking-tight text-white">
                      {memberName}
                    </h1>
                  </div>
                </div>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClassName}`}>
                  {statusKey}
                </span>
              </div>
              <p className="max-w-xs text-sm leading-6 text-slate-300">
                你的手机就是门禁钥匙。保持二维码清晰，靠近入口即可快速完成通行。
              </p>
            </div>
          </div>

          <div className="space-y-6 bg-[linear-gradient(180deg,_#f8fafc_0%,_#eef6ff_100%)] px-6 py-6">
            {profile.subscriptionStatus === 'ACTIVE' ? (
              <div className="rounded-[1.75rem] bg-white p-5 shadow-[0_16px_48px_rgba(15,23,42,0.08)] ring-1 ring-slate-200/80">
                <div className="flex flex-col items-center gap-4 text-center">
                  <div className="rounded-[1.5rem] bg-white p-4 ring-1 ring-slate-200">
                    <QRCodeSVG
                      value={qrToken || 'loading-entrance-token'}
                      size={192}
                      bgColor="transparent"
                      fgColor="#020617"
                      includeMargin={false}
                    />
                  </div>
                  <div className="space-y-1">
                    <p className="text-sm font-medium text-slate-500">动态入馆码</p>
                    <p className="text-xs leading-5 text-slate-400">门禁码仅短时间有效，过期后请立即刷新获取新码。</p>
                    <button
                      type="button"
                      onClick={() => void fetchQrToken()}
                      className="mt-2 rounded-full border border-slate-300 px-3 py-1 text-xs font-semibold text-slate-600 transition hover:bg-slate-50"
                    >
                      刷新门禁码
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <div className="rounded-[1.5rem] bg-slate-900 px-5 py-5 text-sm text-slate-100 shadow-[0_16px_48px_rgba(15,23,42,0.18)]">
                <p>当前订阅状态暂不支持展示入馆二维码，请先恢复有效会员资格。</p>
                <button
                  type="button"
                  onClick={() => void createCheckoutSession()}
                  className="mt-4 rounded-full bg-cyan-400 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300"
                >
                  立即开通会员
                </button>
              </div>
            )}

            <div className="rounded-[1.75rem] bg-white p-5 shadow-[0_16px_48px_rgba(15,23,42,0.08)] ring-1 ring-slate-200/80">
              <div className="flex items-center justify-between gap-3 border-b border-slate-200 pb-4">
                <span className="text-sm font-medium text-slate-500">会员状态</span>
                <span className="text-sm font-semibold text-slate-900">{profile.membershipStatus}</span>
              </div>
              <div className="flex items-center justify-between gap-3 pt-4">
                <span className="text-sm font-medium text-slate-500">到期时间：</span>
                <span className="text-right text-sm font-semibold text-slate-900">
                  {formatDateTime(profile.currentPeriodEndAt)}
                </span>
              </div>
            </div>

            {profile.isCancelAtPeriodEnd ? (
              <div className="rounded-[1.5rem] border border-amber-300/60 bg-amber-50 px-4 py-3 text-sm font-medium leading-6 text-amber-700">
                已申请期末退会，本期结束后将不再扣费
              </div>
            ) : null}

            {errorMessage ? (
              <div className="rounded-[1.5rem] border border-rose-300/60 bg-rose-50 px-4 py-3 text-sm font-medium leading-6 text-rose-700">
                {errorMessage}
              </div>
            ) : null}

            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => void fetchInvoices()}
                className="rounded-[1.25rem] bg-slate-800 px-4 py-3 text-sm font-semibold text-white shadow-[0_12px_30px_rgba(15,23,42,0.22)] transition hover:bg-slate-700"
              >
                历史账单
              </button>
              <button
                type="button"
                onClick={() => void toggleCancel()}
                className={`rounded-[1.25rem] px-4 py-3 text-sm font-semibold transition ${
                  profile.isCancelAtPeriodEnd
                    ? 'bg-amber-400 text-slate-950 shadow-[0_12px_30px_rgba(251,191,36,0.32)] hover:bg-amber-300'
                    : 'border border-rose-300 bg-white text-rose-600 shadow-[0_12px_30px_rgba(244,63,94,0.12)] hover:bg-rose-50'
                }`}
              >
                {profile.isCancelAtPeriodEnd ? '恢复续费' : '申请退会'}
              </button>
            </div>
          </div>
        </div>
      </section>

      <InvoiceModal
        isOpen={isInvoiceModalOpen}
        onClose={() => setIsInvoiceModalOpen(false)}
        invoices={invoices}
      />
    </>
  )
}
