package com.yfzx.service.db.user;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class Email_Auth extends Authenticator  {
	private String username;
	private String password;
	public Email_Auth(String username,String password){
		this.username=username;
		this.password=password;
	}
	protected PasswordAuthentication getPasswordAuthentication() {
	    return new PasswordAuthentication(username, password);
	}
}
