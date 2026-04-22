import type { Invoice } from '../types'

type InvoiceModalProps = {
  isOpen: boolean
  onClose: () => void
  invoices: Invoice[]
}

function formatInvoiceDate(value: string) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 10)
  }

  return date.toISOString().slice(0, 10)
}

function formatAmount(amountPaid: number) {
  return new Intl.NumberFormat('ja-JP', {
    style: 'currency',
    currency: 'JPY',
    maximumFractionDigits: 0,
  }).format(amountPaid)
}

export default function InvoiceModal({ isOpen, onClose, invoices }: InvoiceModalProps) {
  if (!isOpen) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-4 py-6 backdrop-blur-sm">
      <div className="w-full max-w-md overflow-hidden rounded-[2rem] border border-white/10 bg-white shadow-[0_28px_90px_rgba(15,23,42,0.45)]">
        <div className="flex items-center justify-between border-b border-slate-200 bg-slate-950 px-5 py-4 text-white">
          <div>
            <p className="text-xs uppercase tracking-[0.3em] text-cyan-300">Billing History</p>
            <h2 className="mt-2 text-xl font-semibold">历史账单</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-white/15 px-3 py-1 text-sm text-slate-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        <div className="max-h-[70vh] space-y-3 overflow-y-auto bg-[linear-gradient(180deg,_#f8fafc_0%,_#eef6ff_100%)] p-5">
          {invoices.length === 0 ? (
            <div className="rounded-[1.5rem] bg-white px-4 py-6 text-center text-sm text-slate-500 ring-1 ring-slate-200">
              暂无历史账单
            </div>
          ) : (
            invoices.map((invoice) => (
              <div
                key={invoice.id}
                className="flex items-center justify-between gap-4 rounded-[1.5rem] bg-white px-4 py-4 shadow-[0_10px_30px_rgba(15,23,42,0.08)] ring-1 ring-slate-200/80"
              >
                <div className="space-y-1">
                  <p className="text-sm font-semibold text-slate-900">{formatInvoiceDate(invoice.billedAt)}</p>
                  <p className="text-sm text-slate-500">{formatAmount(invoice.amountPaid)}</p>
                </div>
                <span
                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                    invoice.status === 'PAID'
                      ? 'bg-emerald-500/15 text-emerald-600 ring-1 ring-emerald-400/30'
                      : 'bg-slate-500/15 text-slate-500 ring-1 ring-slate-400/30'
                  }`}
                >
                  {invoice.status}
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
