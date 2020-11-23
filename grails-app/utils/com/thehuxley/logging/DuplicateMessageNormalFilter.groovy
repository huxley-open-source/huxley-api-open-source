package com.thehuxley.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.turbo.DuplicateMessageFilter
import ch.qos.logback.core.filter.AbstractMatcherFilter
import ch.qos.logback.core.spi.FilterReply

public class DuplicateMessageNormalFilter extends AbstractMatcherFilter<ILoggingEvent> {

	private final DuplicateMessageFilter delegate;

	public DuplicateMessageNormalFilter() {
		delegate = new DuplicateMessageFilter();
	}

	@java.lang.Override
	void start() {
		super.start()
		delegate.start()
	}

	public FilterReply decide(ILoggingEvent event) {
		return delegate.decide(event.getMarker(), null, event.getLevel(), event.getMessage(), null, null);
	}

}
