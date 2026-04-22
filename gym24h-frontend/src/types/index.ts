export type SubscriptionStatus = 'ACTIVE' | 'ARREARS' | 'CANCELED' | 'EXPIRED' | null

export type MeProfileResponse = {
  displayName: string | null
  membershipStatus: string
  subscriptionStatus: SubscriptionStatus
  subscriptionValidUntil: string | null
  currentPeriodEndAt: string | null
  isCancelAtPeriodEnd: boolean
}

export type Invoice = {
  id: string
  amountPaid: number
  status: string
  billedAt: string
}
