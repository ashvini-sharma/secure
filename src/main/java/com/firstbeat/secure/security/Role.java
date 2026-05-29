package com.firstbeat.secure.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

public class Role implements GrantedAuthority {

	private static final long serialVersionUID = 1L;
	private String name;
	
	public Role(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public @Nullable String getAuthority() {
		return this.name;
	}

}
