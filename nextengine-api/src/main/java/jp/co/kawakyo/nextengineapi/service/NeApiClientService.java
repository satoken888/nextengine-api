package jp.co.kawakyo.nextengineapi.service;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jp.nextengine.api.sdk.NeApiClient;

@Service
public class NeApiClientService {
	  private final Logger logger = LoggerFactory.getLogger(this.getClass());

	  /**
	   * Require login next-engine
	   *
	   * @param request a HttpServletRequest object that contains the request the client
	   * @param response an HttpServletResponse object that contains the response the servlet sends to
	   * the client
	   * @param clientId provided by Next Engine API
	   * @param secretId provided by Next Engine API
	   * @param redirectUrl the callback url when login success
	   * @return the result of the Next Engine API
	   */
	  public HashMap<String, Object> requireLogin(HttpServletRequest request,
	      HttpServletResponse response, String clientId, String secretId, String redirectUrl) {

	    try {
	      NeApiClient neApiClient = new NeApiClient(request, response, clientId, secretId, redirectUrl);
	      return neApiClient.neLogin();
	    } catch (Exception e) {
	      logger.error("error", e);
	    }

	    return null;
	  }

	  /**
	   * Execute api next-engine
	   *
	   * @param apiUrlPath the api path of the Next Engine
	   * @param apiParams params to call the api
	   * @param accessToken the access token of the logged-in user
	   * @param refreshToken the refresh token of the logged-in user
	   * @return the result of the api
	   */
	  public HashMap<String, Object> neApiExecute(String apiUrlPath, HashMap<String, String> apiParams,
	      String accessToken, String refreshToken) {
	    HashMap<String, Object> res = new HashMap<>();
	    try {
	      NeApiClient neApiClient = new NeApiClient(accessToken, refreshToken);
	      res = neApiClient.apiExecute(apiUrlPath, apiParams);
	    } catch (Exception e) {
	      logger.error("error", e);
	    }

	    return res;
	  }

	  /**
	   * Execute api next-engine
	   *
	   * @param apiUrlPath the api path of the Next Engine
	   * @param accessToken the access token of the logged-in user
	   * @param refreshToken the refresh token of the logged-in user
	   * @return the result of the api
	   */
	  public HashMap<String, Object> neApiExecute(String apiUrlPath, String accessToken,
	      String refreshToken) {
	    HashMap<String, Object> res = new HashMap<>();

	    try {
	      NeApiClient neApiClient = new NeApiClient(accessToken, refreshToken);
	      res = neApiClient.apiExecute(apiUrlPath);
	    } catch (Exception e) {
	      logger.error("error", e);
	    }

	    return res;
	  }

}
