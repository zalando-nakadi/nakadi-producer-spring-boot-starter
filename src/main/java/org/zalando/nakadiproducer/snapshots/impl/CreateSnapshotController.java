package org.zalando.nakadiproducer.snapshots.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/events")
public class CreateSnapshotController {

    private static final String CONTENT_TYPE_PROBLEM = "application/problem+json";
    private static final String CONTENT_TYPE_X_PROBLEM = "application/x.problem+json";

    private SnapshotCreationService eventLogService;

    @Autowired
    public CreateSnapshotController(final SnapshotCreationService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @RequestMapping(value = "/snapshots/{event_type:.+}", method = RequestMethod.POST, produces = {CONTENT_TYPE_PROBLEM, CONTENT_TYPE_X_PROBLEM})
    public ResponseEntity<Void> eventsSnapshotPost(
            @PathVariable(value = "event_type") final String eventType) {

        eventLogService.createSnapshotEvents(eventType);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
