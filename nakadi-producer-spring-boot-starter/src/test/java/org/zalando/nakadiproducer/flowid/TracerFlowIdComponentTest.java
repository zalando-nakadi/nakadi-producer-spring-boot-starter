package org.zalando.nakadiproducer.flowid;

import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.zalando.nakadiproducer.flowid.TracerFlowIdComponent;
import org.zalando.tracer.Tracer;

@RunWith(MockitoJUnitRunner.class)
public class TracerFlowIdComponentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Tracer tracer;

    @Test
    public void makeSureItWorks() {
        TracerFlowIdComponent flowIdComponent = new TracerFlowIdComponent(tracer);
        Mockito.when(tracer.get("X-Flow-ID").getValue()).thenReturn("A_FUNKY_VALUE");

        assertThat(flowIdComponent.getXFlowIdKey(), Matchers.equalTo("X-Flow-ID"));
        assertThat(flowIdComponent.getXFlowIdValue(), Matchers.equalTo("A_FUNKY_VALUE"));
    }

}