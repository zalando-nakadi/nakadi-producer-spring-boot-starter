CREATE TABLE tarbela.tarbela_event_log (
  id                  SERIAL            PRIMARY KEY NOT NULL,
  -- event processing status: NEW, SENT, ERROR
  status              TEXT              NOT NULL,
  -- nakadi event type
  event_type          TEXT              NOT NULL,
  -- event payload
  event_body_data     TEXT              NOT NULL,
  -- e.g. warehouse:event
  data_type           TEXT,
  -- data operation: C, U, D, S
  data_op             CHAR(1),
  -- indicates how often a message was sent unsuccessfully
  error_count         INTEGER           NOT NULL DEFAULT 0,
  flow_id             TEXT,
  created             TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
  last_modified       TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

CREATE INDEX tarbela_event_log_status_index ON tarbela.tarbela_event_log (status);
