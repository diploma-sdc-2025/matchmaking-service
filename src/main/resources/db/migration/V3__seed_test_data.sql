-- Seed matchmaking test data for reporting and edge-case coverage.

INSERT INTO matchmaking_history (
  user_id,
  joined_at,
  matched_at,
  left_queue_at,
  match_id,
  wait_time_seconds,
  status,
  cancel_reason
)
VALUES
  (101, CURRENT_TIMESTAMP - INTERVAL '40 minutes', CURRENT_TIMESTAMP - INTERVAL '38 minutes', NULL, 1001, 120, 'MATCHED', NULL),
  (102, CURRENT_TIMESTAMP - INTERVAL '35 minutes', NULL, CURRENT_TIMESTAMP - INTERVAL '34 minutes', NULL, 45, 'LEFT', 'manual_cancel'),
  (103, CURRENT_TIMESTAMP - INTERVAL '25 minutes', NULL, NULL, NULL, NULL, 'WAITING', NULL);

INSERT INTO queue_statistics (
  date,
  total_joins,
  total_matches,
  total_timeouts,
  total_cancellations,
  avg_wait_time_seconds,
  max_wait_time_seconds,
  min_wait_time_seconds,
  peak_queue_size,
  updated_at
)
VALUES
  (CURRENT_DATE - INTERVAL '1 day', 26, 11, 2, 3, 54.20, 180, 8, 9, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (CURRENT_DATE, 14, 6, 1, 1, 47.75, 160, 6, 7, CURRENT_TIMESTAMP)
ON CONFLICT (date) DO UPDATE SET
  total_joins = EXCLUDED.total_joins,
  total_matches = EXCLUDED.total_matches,
  total_timeouts = EXCLUDED.total_timeouts,
  total_cancellations = EXCLUDED.total_cancellations,
  avg_wait_time_seconds = EXCLUDED.avg_wait_time_seconds,
  max_wait_time_seconds = EXCLUDED.max_wait_time_seconds,
  min_wait_time_seconds = EXCLUDED.min_wait_time_seconds,
  peak_queue_size = EXCLUDED.peak_queue_size,
  updated_at = EXCLUDED.updated_at;
