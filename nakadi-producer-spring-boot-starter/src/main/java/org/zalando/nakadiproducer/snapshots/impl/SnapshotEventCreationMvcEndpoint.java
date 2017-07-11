package org.zalando.nakadiproducer.snapshots.impl;

import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.HypermediaDisabled;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public class SnapshotEventCreationMvcEndpoint extends EndpointMvcAdapter {
    private final SnapshotEventCreationEndpoint delegate;

    public SnapshotEventCreationMvcEndpoint(SnapshotEventCreationEndpoint delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @RequestMapping(value = "/{eventType:.*}", method = RequestMethod.POST)
    @ResponseBody
    @HypermediaDisabled
    public ResponseEntity<?> createSnapshot(@PathVariable String eventType,
            @RequestBody(required = false) String filter) {
        if (!this.delegate.isEnabled()) {
            // Shouldn't happen - MVC endpoint shouldn't be registered when delegate's
            // disabled
            return getDisabledResponse();
        }

        delegate.invoke(eventType, filter);
        return ResponseEntity.ok().build();
    }


}
