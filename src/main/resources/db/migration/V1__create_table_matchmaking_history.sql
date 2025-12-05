CREATE TABLE matchmaking_history (
                                     id SERIAL PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     matched_at TIMESTAMP,
                                     left_queue_at TIMESTAMP,
                                     match_id BIGINT,
                                     wait_time_seconds INT,
                                     status VARCHAR(50) NOT NULL DEFAULT 'WAITING',
                                     cancel_reason VARCHAR(255),

                                     CONSTRAINT chk_matchmaking_history_status CHECK (
                                         status IN ('WAITING', 'MATCHED', 'TIMEOUT', 'CANCELLED', 'ERROR')
                                         ),
                                     CONSTRAINT chk_matchmaking_history_matched_logic CHECK (
                                         (status = 'MATCHED' AND matched_at IS NOT NULL AND match_id IS NOT NULL) OR
                                         (status != 'MATCHED')
                                         ),
                                     CONSTRAINT chk_matchmaking_history_cancelled_logic CHECK (
                                         (status = 'CANCELLED' AND cancel_reason IS NOT NULL) OR
                                         (status != 'CANCELLED')
                                         ),
                                     CONSTRAINT chk_matchmaking_history_wait_time CHECK (
                                         wait_time_seconds IS NULL OR wait_time_seconds >= 0
                                         )
);

CREATE INDEX idx_matchmaking_history_user_id ON matchmaking_history(user_id);
CREATE INDEX idx_matchmaking_history_match_id ON matchmaking_history(match_id);
CREATE INDEX idx_matchmaking_history_status ON matchmaking_history(status);
CREATE INDEX idx_matchmaking_history_joined_at ON matchmaking_history(joined_at DESC);
CREATE INDEX idx_matchmaking_history_matched_at ON matchmaking_history(matched_at DESC);

CREATE INDEX idx_matchmaking_history_active_queue
    ON matchmaking_history(user_id, joined_at DESC)
    WHERE status = 'WAITING';