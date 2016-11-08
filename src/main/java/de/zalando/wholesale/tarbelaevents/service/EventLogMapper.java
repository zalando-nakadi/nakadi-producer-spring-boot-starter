package de.zalando.wholesale.tarbelaevents.service;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.zalando.wholesale.tarbelaevents.api.event.model.BunchOfEventsDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchofEventsLinksDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.BunchofEventsLinksNextDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.EventChannelDTO;
import de.zalando.wholesale.tarbelaevents.api.event.model.EventDTO;
import de.zalando.wholesale.tarbelaevents.persistance.entity.EventLog;
import de.zalando.wholesale.tarbelaevents.service.model.NakadiEvent;
import de.zalando.wholesale.tarbelaevents.service.model.NakadiMetadata;
import de.zalando.wholesale.tarbelaevents.web.EventController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Service
@Slf4j
public class EventLogMapper {

    @Autowired
    private ObjectMapper objectMapper;

    public BunchOfEventsDTO mapToDTO(final List<EventLog> events, final String status,
                                     final Integer limit, final String sinkId) {
        final BunchOfEventsDTO bunchOfEventsDTO = new BunchOfEventsDTO();

        final BunchofEventsLinksDTO nextLink = extractNextLink(events, status, limit);
        bunchOfEventsDTO.setLinks(nextLink);

        List<EventDTO> eventDTOs =
            events.stream()      //
                  .map(event -> {
                      final EventDTO eventDTO = new EventDTO();
                      eventDTO.setEventId(String.valueOf(event.getId()));
                      eventDTO.setEventPayload(mapToNakadiPayload(event));
                      eventDTO.setDeliveryStatus(event.getStatus());

                      final EventChannelDTO channel = new EventChannelDTO();
                      channel.setTopicName(event.getEventType());
                      channel.setSinkIdentifier(sinkId);
                      eventDTO.setChannel(channel);

                      return eventDTO;
                  }).collect(Collectors.toList());

        bunchOfEventsDTO.setEvents(eventDTOs);

        return bunchOfEventsDTO;
    }

    /**
     * Returns a with BunchofEventsLinksDTO with new link with the cursor that points to the maximum event id.
     *
     * <p>If events is an empty list then returns null</p>
     */
    private BunchofEventsLinksDTO extractNextLink(final List<EventLog> events, final String status,
            final Integer limit) {

        if (events.isEmpty()) {
            return null;
        }

        int maxEventId = events.stream().map(EventLog::getId).max(Integer::compare).get();
        final String nextUri = linkTo(methodOn(EventController.class).eventsGet(String.valueOf(maxEventId), status,
                    limit)).toString();

        final BunchofEventsLinksNextDTO nextLinkDTO = new BunchofEventsLinksNextDTO();
        nextLinkDTO.setHref(nextUri);

        final BunchofEventsLinksDTO eventsLinksDTO = new BunchofEventsLinksDTO();
        eventsLinksDTO.setNext(nextLinkDTO);

        return eventsLinksDTO;
    }

    private NakadiEvent mapToNakadiPayload(final EventLog event) {

        final NakadiEvent nakadiEvent = new NakadiEvent();

        final NakadiMetadata metadata = new NakadiMetadata();
        metadata.setEid(convertToUUID(event.getId()));
        metadata.setOccuredAt(event.getCreated());
        nakadiEvent.setMetadata(metadata);

        nakadiEvent.setDataOperation(event.getDataOp());
        nakadiEvent.setDataType(event.getDataType());

        HashMap<String, Object> payloadDTO;
        try {
            payloadDTO = objectMapper.readValue(event.getEventBodyData(), Maps.newLinkedHashMap().getClass());
        } catch (IOException e) {
            log.error("An error occurred at JSON deserialization", e);
            throw new UncheckedIOException(e);
        }

        nakadiEvent.setData(payloadDTO);

        return nakadiEvent;
    }

    /**
     * Converts a number in UUID format.
     *
     * <p>For instance 213 will be converted to "00000000-0000-0000-0000-0000000000d5"</p>
     */
    private String convertToUUID(final int number) {
        return new UUID(0, number).toString();
    }
}
