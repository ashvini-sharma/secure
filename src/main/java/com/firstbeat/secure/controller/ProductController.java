package com.firstbeat.secure.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.firstbeat.secure.service.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class ProductController {
	
	@Autowired
	private LoginService loginService;
	
	@GetMapping("/hello/shuru")
	public String demo() {
		return "Demo ho raha hai bhai ..  ";
	}
	
	@PostMapping("/hello/faltu")
	public String faltu() {
		return "aur bhai !!!";
	}
	
	@GetMapping("/check")
	public String check() {
		return "check ho gaya ...";
	}
	
	@PostMapping("/check/case1")
	public String checkCase1() {
		return "check bhi ho gaya ...";
	}
	
	@GetMapping("/login/{username}/{pwd}")
	public boolean login(@PathVariable String username, @PathVariable String pwd, HttpServletRequest req, HttpServletResponse res) {
		return loginService.login(username, pwd, req, res);
	}

}
