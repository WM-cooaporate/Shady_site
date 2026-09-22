import { createContext, useContext } from 'react'
import { usePortfolioData } from '../hooks/usePortfolioData'

const PortfolioContext = createContext(null)

export function PortfolioProvider({ children }) {
  const [data, setData] = usePortfolioData()
  return (
    <PortfolioContext.Provider value={{ data, setData }}>
      {children}
    </PortfolioContext.Provider>
  )
}

export function usePortfolio() {
  const ctx = useContext(PortfolioContext)
  if (!ctx) {
    throw new Error('usePortfolio must be used inside <PortfolioProvider>')
  }
  return ctx
}