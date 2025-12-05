CREATE TABLE queue_statistics (
                                  id SERIAL PRIMARY KEY,
                                  date DATE NOT NULL UNIQUE,
                                  total_joins INT NOT NULL DEFAULT 0,
                                  total_matches INT NOT NULL DEFAULT 0,
                                  total_timeouts INT NOT NULL DEFAULT 0,
                                  total_cancellations INT NOT NULL DEFAULT 0,
                                  avg_wait_time_seconds NUMERIC(10,2) NOT NULL DEFAULT 0,
                                  max_wait_time_seconds INT NOT NULL DEFAULT 0,
                                  min_wait_time_seconds INT NOT NULL DEFAULT 0,
                                  peak_queue_size INT NOT NULL DEFAULT 0,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_queue_statistics_date ON queue_statistics(date DESC);
CREATE INDEX idx_queue_statistics_updated_at ON queue_statistics(updated_at DESC);
