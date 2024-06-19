package org.eclipse.cargotracker.interfaces.booking.web;

import java.io.Serializable;
import javax.enterprise.inject.Produces;
import javax.faces.flow.Flow;
import javax.faces.flow.builder.FlowBuilder;
import javax.faces.flow.builder.FlowBuilderParameter;
import javax.faces.flow.builder.FlowDefinition;

public class BookingFlow implements Serializable {

    @Produces
    @FlowDefinition
    public Flow defineFlow(@FlowBuilderParameter FlowBuilder flowBuilder) {
        String flowId = "booking";
        flowBuilder.id("", flowId);
        flowBuilder.viewNode(flowId, "/booking/booking.xhtml").markAsStartNode();
        flowBuilder.viewNode("booking-destination", "/booking/booking-destination.xhtml");
        flowBuilder.viewNode("booking-date", "/booking/booking-date.xhtml");
        flowBuilder.returnNode("returnFromBookingFlow").fromOutcome("/admin/dashboard.xhtml");
        
        return flowBuilder.getFlow();
    }
}