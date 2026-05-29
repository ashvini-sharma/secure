package com.firstbeat.secure.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.firstbeat.secure.security.Role;

@Configuration
public class MySecurityConfiguration {
	
	//@Bean
	AuthenticationProvider authenticationProviderWithLogic() {
		return new AuthenticationProvider() {

			@Override
			public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
				String username = authentication.getName();
				String pwd = authentication.getCredentials().toString();
				if(username.startsWith("ashv")&& pwd.equals("mypassword123"))
					return new UsernamePasswordAuthenticationToken(username, pwd, Arrays.asList());
				else 
					throw new BadCredentialsException("UserName sahi se daalo"); 
			}

			@Override
			public boolean supports(Class<?> authentication) {
				return authentication.equals(UsernamePasswordAuthenticationToken.class);
			}
			
		};
	}
	
	@Bean
	AuthenticationProvider authenticationProvider() {
	    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
	    provider.setPasswordEncoder(encoder());
	    return provider;
	}
	
	@Bean
	UserDetailsService userDetailsService() {
		UserDetails ashvini = User.withUsername("ashvini").password(encoder().encode("admin123")).roles("ADMIN-HIGH", "USER-LOW").build();
		UserDetails nikhil = User.withUsername("nikhil").password(encoder().encode("user123")).roles("USER-LOW").build();
		InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(ashvini, nikhil);
		return manager;
	}
	
	@Bean
	BCryptPasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean  // ashvini/admin123 (ADMIN) has all the access but nikhil/user123 (USER) has access to only GET /hello/shuru 
	SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
		httpSecurity.csrf(csrf -> csrf.disable())
		            //.httpBasic(Customizer.withDefaults())
		            .authorizeHttpRequests(T -> 
		                   T.requestMatchers(HttpMethod.GET, "/hello/shuru").hasAnyRole("ADMIN-HIGH","USER-LOW")
		                    .requestMatchers(HttpMethod.GET, "/check").hasAnyRole("ADMIN-HIGH")
		                    .requestMatchers(HttpMethod.POST, "/hello/faltu", "/check/*").hasAnyRole("ADMIN-HIGH")
		                    .requestMatchers("/login/**").permitAll()
		            		);
		
		httpSecurity.securityContext((securityContext) -> securityContext.requireExplicitSave(true));
		return httpSecurity.build();
	}
	
	@Bean
	SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(), new HttpSessionSecurityContextRepository());
	}

}
