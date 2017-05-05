CREATE TABLE nakadi_events.event_log (
  id                  SERIAL            PRIMARY KEY NOT NULL,
  -- nakadi event type
  event_type          TEXT              NOT NULL,
  -- event payload
  event_body_data     TEXT              NOT NULL,
  -- e.g. warehouse:event
  data_type           TEXT,
  -- data operation: C, U, D, S
  data_op             CHAR(1),
  flow_id             TEXT,
  created             TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
  last_modified       TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
  locked_by           TEXT,
  locked_until        TIMESTAMPTZ
);

CREATE INDEX event_log_locked_until_index ON nakadi_events.event_log (locked_until);