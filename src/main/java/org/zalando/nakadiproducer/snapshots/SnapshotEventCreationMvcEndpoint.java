package org.zalando.nakadiproducer.snapshots;

import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.HypermediaDisabled;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
    public Object createSnapshot(@PathVariable String eventType) {
        if (!this.delegate.isEnabled()) {
            // Shouldn't happen - MVC endpoint shouldn't be registered when delegate's
            // disabled
            return getDisabledResponse();
        }

        delegate.invoke(eventType);
        return ResponseEntity.ok().build();
    }


}
