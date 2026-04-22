import liff from '@line/liff'
import { createContext, useContext, useEffect, useState, type PropsWithChildren } from 'react'

type LiffContextValue = {
  liff: typeof liff
  isLoggedIn: boolean
  isLoading: boolean
}

const LiffContext = createContext<LiffContextValue | undefined>(undefined)

export function LiffProvider({ children }: PropsWithChildren) {
  const [isLoading, setIsLoading] = useState(true)
  const [isLoggedIn, setIsLoggedIn] = useState(false)

  useEffect(() => {
    let isMounted = true

    async function initializeLiff() {
      try {
        await liff.init({ liffId: 'mock-liff-id' })

        if (!isMounted) {
          return
        }

        setIsLoggedIn(liff.isLoggedIn())
      } catch (error) {
        console.warn('LIFF init 失败，已降级为开发模式:', error)

        if (!isMounted) {
          return
        }

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
