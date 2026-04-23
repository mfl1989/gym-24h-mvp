import liff from '@line/liff'
import api from '../api/axios'
import { createContext, useContext, useEffect, useState, type PropsWithChildren } from 'react'

const DEV_LOGIN_USER_ID = '11111111-1111-1111-1111-111111111111'

type LiffContextValue = {
  liff: typeof liff
  isLoggedIn: boolean
  isLoading: boolean
}

type AuthTokenPayload = {
  authToken: string
  expiresAt: string
}

type ApiResponse<T> = {
  success: boolean
  data: T
}

const LiffContext = createContext<LiffContextValue | undefined>(undefined)

const LIFF_ID = import.meta.env.VITE_LIFF_ID

function isLocalDevelopment() {
  return window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
}

export function LiffProvider({ children }: PropsWithChildren) {
  const [isLoading, setIsLoading] = useState(true)
  const [isLoggedIn, setIsLoggedIn] = useState(() => Boolean(localStorage.getItem('authToken')))

  useEffect(() => {
    let isMounted = true

    async function initializeLiff() {
      if (window.location.pathname.startsWith('/admin/')) {
        if (isMounted) {
          setIsLoading(false)
        }
        return
      }

      try {
        if (isLocalDevelopment()) {
          const response = await api.post<ApiResponse<AuthTokenPayload>>('/auth/dev-login', {
            userId: DEV_LOGIN_USER_ID,
          })

          if (!isMounted) {
            return
          }

          localStorage.setItem('authToken', response.data.data.authToken)
          setIsLoggedIn(true)
          return
        }

        if (!LIFF_ID) {
          const error = new Error('未配置 VITE_LIFF_ID')
          console.error(error)
          throw error
        }

        await liff.init({ liffId: LIFF_ID })

        if (!liff.isLoggedIn()) {
          liff.login()
          return
        }

        const lineIdToken = liff.getIDToken()

        if (!lineIdToken) {
          throw new Error('LINE ID token is unavailable')
        }

        const response = await api.post<ApiResponse<AuthTokenPayload>>('/auth/line-login', {
          idToken: lineIdToken,
        })

        if (!isMounted) {
          return
        }

        localStorage.setItem('authToken', response.data.data.authToken)
        setIsLoggedIn(true)
      } catch (error) {
        console.warn('LIFF init failed:', error)

        if (!isMounted) {
          return
        }

        localStorage.removeItem('authToken')
        setIsLoggedIn(false)
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    void initializeLiff()

    return () => {
      isMounted = false
    }
  }, [])

  return (
    <LiffContext.Provider
      value={{
        liff,
        isLoggedIn,
        isLoading,
      }}
    >
      {children}
    </LiffContext.Provider>
  )
}

export function useLiff() {
  const context = useContext(LiffContext)

  if (!context) {
    throw new Error('useLiff must be used within a LiffProvider')
  }

  return context
}
