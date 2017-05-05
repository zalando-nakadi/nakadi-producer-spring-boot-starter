GRANT USAGE ON SCHEMA nakadi_events TO PUBLIC;
GRANT SELECT ON nakadi_events.event_log TO PUBLIC;
GRANT INSERT ON nakadi_events.event_log TO PUBLIC;
GRANT UPDATE ON nakadi_events.event_log TO PUBLIC;
GRANT USAGE ON SEQUENCE nakadi_events.event_log_id_seq TO PUBLIC;