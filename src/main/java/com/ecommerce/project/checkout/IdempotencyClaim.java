package com.ecommerce.project.checkout;

// Either a fresh (or reclaimed-after-failure) claim the caller should now act on, or a replay of
// a previously completed response the caller should return as-is without redoing any work.
public final class IdempotencyClaim {

    private final Long recordId;
    private final boolean replay;
    private final String replayResponseBody;

    private IdempotencyClaim(Long recordId, boolean replay, String replayResponseBody) {
        this.recordId = recordId;
        this.replay = replay;
        this.replayResponseBody = replayResponseBody;
    }

    static IdempotencyClaim begin(Long recordId) {
        return new IdempotencyClaim(recordId, false, null);
    }

    static IdempotencyClaim replay(String responseBody) {
        return new IdempotencyClaim(null, true, responseBody);
    }

    public boolean isReplay() {
        return replay;
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getReplayResponseBody() {
        return replayResponseBody;
    }
}
