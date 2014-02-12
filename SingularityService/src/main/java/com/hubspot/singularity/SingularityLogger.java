package com.hubspot.singularity;

import org.slf4j.LoggerFactory;

import com.hubspot.singularity.config.SingularityConfiguration;

import com.google.inject.Inject;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext; 

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.RavenFactory;
import net.kencochrane.raven.logback.SentryAppender;

public class SingularityLogger {
	
	private final SentryAppender sentryAppender;
	
	private final Raven raven;
	
	private final String dsn;
	
	@Inject
	SingularityLogger (SingularityConfiguration singularityConfiguration) { 
		this.dsn = singularityConfiguration.getSentryConfiguration().getDsn();
		this.raven = RavenFactory.ravenInstance(dsn);
		this.sentryAppender = new SentryAppender(this.raven);	
	}
	
	public Logger getLogger(Class cls){
		Logger LOG = (Logger) LoggerFactory.getLogger(cls);
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
	
		sentryAppender.setContext(loggerContext);
		sentryAppender.start();
		
		LOG.addAppender(sentryAppender);
		
		return LOG;
				
	}
}
