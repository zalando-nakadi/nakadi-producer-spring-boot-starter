package org.zalando.nakadiproducer.eventlog;

import java.util.Collection;
import java.util.Map;
import javax.transaction.Transactional;

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
   * @param dataTypeToData
   *            the content of the {@code data_type} field of the Nakadi
   *            event mapped to some POJOs that can be serialized into JSON (required
   *            parameter). This is meant to be a representation of the
   *            current state of the resource. It will be used as content of
   *            the {@code data} field of the Nakadi event.
   */
    @Transactional
    void fireCreateEvents(String eventType, Map<String, Collection<Object>> dataTypeToData);

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
   * @param dataTypeToData
   *            the content of the {@code data_type} field of the Nakadi
   *            event mapped to some POJOs that can be serialized into JSON (required
   *            parameter). This is meant to be a representation of the
   *            current state of the resource. It will be used as content of
   *            the {@code data} field of the Nakadi event.
   */
    @Transactional
    void fireUpdateEvents(String eventType, Map<String, Collection<Object>> dataTypeToData);

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
   * @param dataTypeToData
   *            the content of the {@code data_type} field of the Nakadi
   *            event mapped to some POJOs that can be serialized into JSON (required
   *            parameter). This is meant to be a representation of the
   *            current state of the resource. It will be used as content of
   *            the {@code data} field of the Nakadi event.
   */
    @Transactional
    void fireDeleteEvents(String eventType, Map<String, Collection<Object>> dataTypeToData);

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
   * @param dataTypeToData
   *            the content of the {@code data_type} field of the Nakadi
   *            event mapped to some POJOs that can be serialized into JSON (required
   *            parameter). This is meant to be a representation of the
   *            current state of the resource. It will be used as content of
   *            the {@code data} field of the Nakadi event.
   */
    @Transactional
    void fireSnapshotEvents(String eventType, Map<String, Collection<Object>> dataTypeToData);

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
