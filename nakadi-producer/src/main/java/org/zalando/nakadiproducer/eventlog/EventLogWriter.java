package org.zalando.nakadiproducer.eventlog;

import java.util.Collection;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import org.zalando.nakadiproducer.snapshots.SnapshotEventGenerator;

/**
 * The main user interface for this library. Autowire an instance of this
 * interface into your service, and call one of the methods whenever you want to
 * send an event.
 * <p>
 * All the methods are supposed to be called inside the same database
 * transaction which also contains the corresponding changes to your business
 * objects. This way it is made sure that the events are persisted if and only
 * if the containing transaction is successful, sidestepping the distributed
 * transaction problem.
 * </p>
 * <p>
 * The library will later try to submit all those persisted events to Nakadi.
 * </p>
 */
public interface EventLogWriter {

    /**
     * Registers a function for extracting compaction keys for events sent in the future.
     * <p>
     * When an event for this event type is sent out, and its `data` (or `payload`) object matches the type,
     * the extractor is used to set the {@code partition_compaction_key} in the event to be sent out.
     * For all other objects (or if the extractor returns null), no compaction key is set.
     * </p>
     *<p>
     * (If multiple extractors are registered for the same event type, their data
     *  type is checked in reverse order of registering, until a fitting one is found.
     *  The first with matching dataType is used.)
     * </p>
     * <dl><dt>Note</dt>
     * <dd>
     *     Nakadi-Producer does <b>not</b> guarantee event ordering,
     *     which is fundamentally incompatible with Nakadi's log-compaction mechanism,
     *     as Nakadi will always keep just the last submitted event for the same compaction key,
     *     which might not be the last produced one.
     *     Still, in some special cases (e.g. when there is normally a long time between production
     *     of events with the same compaction key, so all events will be sent out before the next group)
     *     it is possible without problems (and these are also cases where the compaction is most useful).
     *     <b>This feature is meant just for these special cases.</b>
     *     </dd></dl>
     * </p>
     *
     *
     * @param eventType the event type for which this is used.
     * @param dataType the Java type of the objects for which the extractor is used.
     * @param extractor a function which will extract the compaction key from an object.
     */
    <X> void registerCompactionKeyExtractor(String eventType, Class<X> dataType, CompactionKeyExtractor<X> extractor);

    /**
     * Fires a data change event about a <b>creation</b> of some resource
     * (object).
     *
     * @param eventType
     *            the Nakadi event type of the event. This is roughly equivalent
     *            to an event channel or topic.
     *
     * @param dataType
     *            the content of the {@code data_type} field of the Nakadi
     *            event.
     *
     * @param data
     *            some POJO that can be serialized into JSON (required
     *            parameter). This is meant to be a representation of the
     *            resource which was created. It will be used as content of the
     *            {@code data} field of the Nakadi event.
     */
    @Transactional
    void fireCreateEvent(String eventType, String dataType, Object data);

  /**
   * Fires data change events about the creation of some resources (objects), see
   * {@link #fireCreateEvent(String, String, Object) fireCreateEvent} for more details.
   *
   * @param eventType
   *            the Nakadi event type of the event. This is roughly equivalent
   *            to an event channel or topic.
   *
   * @param dataType
   *            the content of the {@code data_type} field of the Nakadi
   *            event
   *
   * @param data
   *            some POJOs that can be serialized into JSON (required
   *            parameter). This is meant to be a representation of the
   *            current state of the resource. It will be used as content of
   *            the {@code data} field of the Nakadi event.
   */
    @Transactional
    void fireCreateEvents(String eventType, String dataType, Collection<?> data);

    /**
     * Fires a data change event about an update of some resource (object).
     *
     * @param eventType
     *            the Nakadi event type of the event. This is roughly equivalent
     *            to an event channel or topic.
     *
     * @param dataType
     *            the content of the {@code data_type} field of the Nakadi
     *            event.
     *
     * @param data
     *            some POJO that can be serialized into JSON (required
     *            parameter). This is meant to be a representation of the new
     *            state of the resource which was updated. It will be used as
     *            content of the {@code data} field of the Nakadi event.
     */
    @Transactional
    void fireUpdateEvent(String eventType, String dataType, Object data);

  /**
   * Fires data change events about the update of some resources (objects), see
   * {@link #fireUpdateEvent(String, String, Object) fireUpdateEvent} for more details.
   *
   * @param eventType
   *            the Nakadi event type of the event. This is roughly equivalent
   *            to an event channel or topic.
   *
   * @param dataType
   *            the content of the {@code data_type} field of the Nakadi
   *            event
   *
   * @param data
   *            some POJOs that can be serialized into JSON (required
   *            parameter). This is meant to be a representation of the
   *            current state of the resource. It will be used as content of
   *            the {@code data} field of the Nakadi event.
   */
    @Transactional
    void fireUpdateEvents(String eventType, String dataType, Collection<?> data);

    /**
     * Fires a data change event about the deletion of some resource (object).
     *
     * @param eventType
     *            the Nakadi event type of the event. This is roughly equivalent
     *            to an event channel or topic.
     *
     * @param dataType
     *            the content of the {@code data_type} field of the Nakadi
     *            event.
     *
     * @param data
     *            some POJO that can be serialized into JSON (required
     *            parameter). This is meant to be a representation of the last
     *            state (before the deletion) of the resource which was deleted.
     *            It will be used as content of the {@code data} field of the
     *            Nakadi event.
     */
    @Transactional
    void fireDeleteEvent(String eventType, String dataType, Object data);

  /**
   * Fires data change events about the deletion of some resources (objects), see
   * {@link #fireDeleteEvent(String, String, Object) fireDeleteEvent} for more details.
   *
   * @param eventType
   *            the Nakadi event type of the event. This is roughly equivalent
   *            to an event channel or topic.
   *
   * @param dataType
   *            the content of the {@code data_type} field of the Nakadi
   *            event
   *
   * @param data
   *            some POJOs that can be serialized into JSON (required
   *            parameter). This is meant to be a representation of the
   *            current state of the resource. It will be used as content of
   *            the {@code data} field of the Nakadi event.
   */
    @Transactional
    void fireDeleteEvents(String eventType, String dataType, Collection<?> data);

    /**
     * Fires a data change event with a snapshot of some resource (object).
     * <p>
     * This notifies your consumers about the current state of a resource, even
     * if nothing changed. Typical use cases include initial replication to new
     * consumers or hotfixes of data inconsistencies between producer and
     * consumer.
     * </p>
     * <p>
     * Normally applications don't have to call this themselves, instead they
     * should implement the
     * {@link SnapshotEventGenerator}
     * interface to add support for snapshot creation via the actuator endpoint.
     * </p>
     *
     * @param eventType
     *            the Nakadi event type of the event. This is roughly equivalent
     *            to an event channel or topic.
     *
     * @param dataType
     *            the content of the {@code data_type} field of the Nakadi
     *            event.
     *
     * @param data
     *            some POJO that can be serialized into JSON (required
     *            parameter). This is meant to be a representation of the
     *            current state of the resource. It will be used as content of
     *            the {@code data} field of the Nakadi event.
     */
    @Transactional
    void fireSnapshotEvent(String eventType, String dataType, Object data);

  /**
   * Fires data change events, see {@link #fireSnapshotEvent(String, String, Object)
   * fireSnapshotEvent} for more details.
   *
   * @param eventType
   *            the Nakadi event type of the event. This is roughly equivalent
   *            to an event channel or topic.
   *
   * @param dataType
   *            the content of the {@code data_type} field of the Nakadi
   *            event
   *
   * @param data
   *            some POJOs that can be serialized into JSON (required
   *            parameter). This is meant to be a representation of the
   *            current state of the resource. It will be used as content of
   *            the {@code data} field of the Nakadi event.
   */
    @Transactional
    void fireSnapshotEvents(String eventType, String dataType, Collection<?> data);

    /**
     * Fires a business event, i.e. an event communicating the fact that some
     * business process step happened. The payload object will be used as the
     * main event content (just metadata will be added). Same as for data change
     * events, you should call this method in the same transaction as you are
     * storing related changes into your database.
     *
     * @param eventType
     *            the Nakadi event type of the event. This is roughly equivalent
     *            to an event channel or topic.
     *
     * @param payload
     *            some POJO that can be serialized into JSON (required
     *            parameter)
     */
    @Transactional
    void fireBusinessEvent(String eventType, Object payload);

  /**
   * Fires business events, see {@link #fireBusinessEvent(String, Object) fireBusinessEvent} for
   * more details
   *
   * @param eventType
   *            the Nakadi event type of the event. This is roughly equivalent
   *            to an event channel or topic.
   *
   * @param payloads
   *            some POJOs that can be serialized into JSON (required
   *            parameter)
   */
    @Transactional
    void fireBusinessEvents(String eventType, Collection<Object> payloads);
}
