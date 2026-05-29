package com.firstbeat.secure.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class LoginService {
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Autowired
	private AuthenticationProvider authenticationProvider;
	
	@Autowired
	private SecurityContextRepository securityContextRepo;
	
	public boolean login(String username, String pwd, HttpServletRequest req, HttpServletResponse res) {
		UserDetails userdetails = userDetailsService.loadUserByUsername(username);
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userdetails, pwd, userdetails.getAuthorities());
		authenticationProvider.authenticate(token);
		boolean isAuthenticated = token.isAuthenticated();
		if(isAuthenticated) {
			SecurityContext context = SecurityContextHolder.getContext();
			context.setAuthentication(token);
			securityContextRepo.saveContext(context, req, res);
		}
		
		return isAuthenticated;
	}

}
