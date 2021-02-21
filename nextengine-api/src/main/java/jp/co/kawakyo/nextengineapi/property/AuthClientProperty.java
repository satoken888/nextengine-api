package jp.co.kawakyo.nextengineapi.property;

import lombok.Data;

@Data
public class AuthClientProperty {

	private String clientId;
	private String clientSecret;
	private String redirectUrl;
	private String domainPath;
}
