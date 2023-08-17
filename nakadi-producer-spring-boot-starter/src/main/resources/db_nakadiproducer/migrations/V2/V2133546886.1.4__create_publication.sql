CREATE PUBLICATION nakadi_producer_eventlog
  FOR TABLE nakadi_events.event_log;

COMMENT ON PUBLICATION nakadi_producer_eventlog
 IS 'Publication for collecting event data data via Fabric Event Streams.'
    'If you didn''t set FES up, it won''t be used and do no harm.';
