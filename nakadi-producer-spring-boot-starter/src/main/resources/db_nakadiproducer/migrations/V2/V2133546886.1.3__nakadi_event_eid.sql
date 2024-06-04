ALTER TABLE nakadi_events.event_log ADD COLUMN eid uuid DEFAULT CAST(LPAD(TO_HEX(currval('nakadi_events.event_log_id_seq')), 32, '0') AS UUID);
