package org.zalando.nakadiproducer.web;


import org.zalando.nakadiproducer.service.EventLogService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/events")
public class EventController {

    private static final String CONTENT_TYPE_PROBLEM = "application/problem+json";
    private static final String CONTENT_TYPE_X_PROBLEM = "application/x.problem+json";

    private EventLogService eventLogService;

    private FlowIdComponent flowIdComponent;

    @Autowired
    public EventController(final EventLogService eventLogService, FlowIdComponent flowIdComponent) {
        this.eventLogService = eventLogService;
        this.flowIdComponent = flowIdComponent;
    }

    @RequestMapping(value = "/snapshots/{event_type:.+}", method = RequestMethod.POST, produces = {CONTENT_TYPE_PROBLEM, CONTENT_TYPE_X_PROBLEM})
    public ResponseEntity<Void> eventsSnapshotPost(
            @PathVariable(value = "event_type") final String eventType) {

        eventLogService.createSnapshotEvents(eventType, flowIdComponent.getXFlowIdValue());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
