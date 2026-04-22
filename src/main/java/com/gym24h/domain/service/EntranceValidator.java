package com.gym24h.domain.service;

import com.gym24h.domain.model.subscription.Subscription;

import java.time.Instant;

/**
 * 入館可否を判定する純粋なドメインサービス。
 *
 * 業務上の重要点は、決済直後や期限直前の境界で利用者体験を損なわないことにある。
 * そのため、端末時刻やネットワーク遅延の微小な差分で正規会員を締め出さないよう、
 * 判定には固定の時間バッファを持たせる。
 */
public class EntranceValidator {

    private static final long DEFAULT_BUFFER_SECONDS = 10L;

    public void validate(Subscription subscription, Instant requestedAt) {
        // 入館判定を strict equality にすると、端末時計の揺れだけで正規会員が失敗する。
        // 無人運営ではその場で有人救済できないため、ビジネスルールとして 10 秒の許容幅を設ける。
        if (!subscription.canEnter(requestedAt, DEFAULT_BUFFER_SECONDS)) {
            throw new IllegalStateException("Subscription is not eligible for entrance");
        }
    }
}
