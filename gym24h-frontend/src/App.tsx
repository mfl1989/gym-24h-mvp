import { LiffProvider, useLiff } from './contexts/LiffContext'
import Dashboard from './components/Dashboard'
import Login from './components/Login'
import AdminScanner from './pages/AdminScanner'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const authToken = localStorage.getItem('authToken')

  if (!authToken) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}

function AppContent() {
  const { isLoading } = useLiff()

  if (isLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-950 text-center text-xl font-medium text-white">
        加载中...
      </main>
    )
  }

  return (
    <BrowserRouter>
      <main className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(34,211,238,0.22),_transparent_30%),linear-gradient(180deg,_#020617_0%,_#111827_100%)] text-white">
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/admin/scanner" element={<AdminScanner />} />
          <Route
            path="/dashboard"
            element={
              <RequireAuth>
                <Dashboard />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </BrowserRouter>
  )
}

function App() {
  return (
    <LiffProvider>
      <AppContent />
    </LiffProvider>
  )
}

export default App
