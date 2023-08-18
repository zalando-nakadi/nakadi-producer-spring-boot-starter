-- We only need insert. Updates will happen when this library itself
 -- tries to send things out, but that should not be published.
CREATE PUBLICATION nakadi_producer_fes
   FOR TABLE nakadi_events.event_log
   WITH (publish = 'insert');

COMMENT ON PUBLICATION nakadi_producer_fes
     IS 'Publication for collecting event data data via Fabric Event Streams.'
        'If you didn''t set FES up, it won''t be used and do no harm.';

-- We drop the slot in case it already exists (e.g. created by Patroni from the database manifest),
--  as it needs to be created after the publication.
SELECT pg_drop_replication_slot(slot_name)
  FROM pg_replication_slots
 WHERE slot_name = 'nakadi_producer_fes';
