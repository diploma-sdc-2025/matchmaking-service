CREATE TABLE matchmaking_history (
                                     id SERIAL PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     matched_at TIMESTAMP,
                                     left_queue_at TIMESTAMP,
                                     match_id BIGINT,
                                     wait_time_seconds INT,
                                     status VARCHAR(50) NOT NULL DEFAULT 'WAITING',
                                     cancel_reason VARCHAR(255)
);

CREATE INDEX idx_matchmaking_history_user_id ON matchmaking_history(user_id);
CREATE INDEX idx_matchmaking_history_match_id ON matchmaking_history(match_id);
CREATE INDEX idx_matchmaking_history_status ON matchmaking_history(status);
