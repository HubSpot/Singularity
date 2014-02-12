package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

public class SentryConfiguration {

	@NotNull
	private String dsn;
	
	@NotNull
	private String level;
	
	public String getDsn(){
		return this.dsn;
	}
	
	public String getLevel(){
		return this.level;
	}
	
}
