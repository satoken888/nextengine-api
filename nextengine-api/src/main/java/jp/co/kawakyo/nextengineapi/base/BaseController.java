package jp.co.kawakyo.nextengineapi.base;

import java.util.HashMap;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kawakyo.nextengineapi.property.AuthClientProperty;
import jp.co.kawakyo.nextengineapi.service.NeApiClientService;
import jp.co.kawakyo.nextengineapi.utils.Constant;
import jp.co.kawakyo.nextengineapi.utils.NeApiClient;
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;

public abstract class BaseController {

	public Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public NeApiClientService neApiClientService;

	@Autowired
	public AuthClientProperty authClientProperty;

	@Autowired
	public MessageSource messageSource;

	public BaseController() {

	}

	/**
	 * Get current user from session
	 *
	 * @return the current Authentication Token
	 */
	public NeToken getCurrentToken(HttpServletRequest request) {

		return (NeToken) request.getSession().getAttribute(NeToken.class.getName());
	}

	/**
	 * Login next-engine
	 *
	 * @param request  a HttpServletRequest object that contains the request the
	 *                 client
	 * @param response an HttpServletResponse object that contains the response the
	 *                 servlet sends to the client
	 * @return information of Logged-In User
	 */
	public HashMap<String, Object> neLogin(HttpServletRequest request, HttpServletResponse response,
			String referrerPath) {
		return neApiClientService.requireLogin(request, response, authClientProperty.getClientId(),
				authClientProperty.getClientSecret(), referrerPath);
	}

	/**
	 * Call API by Token with params
	 *
	 * @param token      current authentication token
	 * @param apiUrlPath Next Engine API Path
	 * @param apiParams  Params to call the Next Engine API
	 * @return the result of the Next Engine API
	 * @throws InstantiationException if has errors when the specified class object
	 *                                cannot be instantiated
	 * @throws IllegalAccessException if has errors when does not have access to the
	 *                                definition of the specified class
	 */
	public HashMap<String, Object> neApiExecute(NeToken token,
			String apiUrlPath,
			HashMap<String, String> apiParams) {
		if (apiParams == null) {
			return neApiClientService.neApiExecute(apiUrlPath, token.getAccessToken(), token.getRefreshToken());
		} else {
			return neApiClientService.neApiExecute(apiUrlPath, apiParams, token.getAccessToken(),
					token.getRefreshToken());
		}
	}

	/**
	 * Call API by Token with params and save token to session and database
	 *
	 * @param request    a HttpServletRequest object that contains the request the
	 *                   client
	 * @param token      the current authentication token of current logged-in user
	 * @param apiUrlPath the api of Next Engine
	 * @param apiParams  the params to call the api
	 * @return the result of the api
	 * @throws InstantiationException if has errors when the specified class object
	 *                                cannot be instantiated
	 * @throws IllegalAccessException if has errors when does not have access to the
	 *                                definition of the specified class
	 */
	public HashMap<String, Object> neApiExecuteAndSaveToken(HttpServletRequest request,
			NeToken token,
			String apiUrlPath,
			HashMap<String, String> apiParams)
			throws IllegalAccessException, InstantiationException {

		HashMap<String, Object> apiResponse = neApiExecute(token, apiUrlPath, apiParams);
		saveToken(request, token, apiResponse);

		return apiResponse;
	}

	/**
	 * Update Token if token changed
	 *
	 * @param request      a HttpServletRequest object that contains the request the
	 *                     client
	 * @param currentToken current Authentication Token of Logged-In User
	 * @param apiResponse  the result of the API
	 * @throws InstantiationException if has errors when the specified class object
	 *                                cannot be instantiated
	 * @throws IllegalAccessException if has errors when does not have access to the
	 *                                definition of the specified class
	 */
	public void saveToken(HttpServletRequest request, NeToken currentToken,
			HashMap<String, Object> apiResponse) throws InstantiationException, IllegalAccessException {
		Object token = apiResponse.get(NeApiClient.KEY_ACCESS_TOKEN);
		if (token == null) {
			return;
		}
		if (currentToken != null && token.toString().equals(currentToken.getAccessToken())) {
			return;
		}
		saveTokenToSession(request, currentToken, apiResponse);
		// C company =
		// createCompany(apiResponse.get(NeApiClient.KEY_ACCESS_TOKEN).toString(),
		// apiResponse.get(NeApiClient.KEY_REFRESH_TOKEN).toString());
		// createUser(company.getId(),
		// apiResponse.get(NeApiClient.KEY_ACCESS_TOKEN).toString(),
		// apiResponse.get(NeApiClient.KEY_REFRESH_TOKEN).toString());
	}

	/**
	 * Update token to session
	 *
	 * @param request     a HttpServletRequest object that contains the request the
	 *                    client
	 * @param token       current Authentication Token of Logged-In User
	 * @param apiResponse the result of the API
	 */
	public void saveTokenToSession(HttpServletRequest request, NeToken token, HashMap<String, Object> apiResponse) {
		NeToken neToken = token;
		if (token == null) {
			neToken = new NeToken();
		}
		neToken.setAccessToken(apiResponse.get(NeApiClient.KEY_ACCESS_TOKEN).toString());
		neToken.setRefreshToken(apiResponse.get(NeApiClient.KEY_REFRESH_TOKEN).toString());
		request.getSession().setAttribute(NeToken.class.getName(), neToken);
	}

	/**
	 * Get company info of login user from api /api_v1_login_company/info
	 *
	 * @param accessToken  current Access Token of Logged-In User
	 * @param refreshToken current Refresh Token of Logged-In User
	 * @return the result of the API /api_v1_login_company/info
	 */
	public HashMap<String, Object> fetchCompanyInfo(String accessToken, String refreshToken) {
		return neApiClientService.neApiExecute(NeApiURL.COMPANY_INFO_PATH,
				accessToken, refreshToken);
	}

	/**
	 * Get login user info from api /api_v1_login_user/info
	 *
	 * @param accessToken  current Access Token of Logged-In User
	 * @param refreshToken current Refresh Token of Logged-In User
	 * @return the result of the API /api_v1_login_user/info
	 */
	public HashMap<String, Object> fetchUserInfo(String accessToken, String refreshToken) {
		return neApiClientService.neApiExecute(NeApiURL.USER_INFO_PATH,
				accessToken, refreshToken);
	}

	/**
	 * Get messages from resources/messages_us.properties
	 *
	 * @param messages the message key to get
	 * @return the message from resources
	 */
	public String getMessage(String messages) {
		return messageSource.getMessage(messages, new String[] {}, Locale.JAPAN);
	}

	/**
	 * Create and return Company model base on information that obtained from the
	 * API
	 *
	 * @param accessToken  current Access Token of Logged-In User
	 * @param refreshToken current Refresh Token of Logged-In User
	 * @return The Company Object that contains company information
	 * @throws InstantiationException if has errors when the specified class object
	 *                                cannot be instantiated
	 * @throws IllegalAccessException if has errors when does not have access to the
	 *                                definition of the specified class
	 */
	// C createCompany(String accessToken, String refreshToken) throws
	// InstantiationException, IllegalAccessException{
	// C company = companyClass.newInstance();
	// HashMap<String, Object> apiResponse = fetchCompanyInfo(accessToken,
	// refreshToken);
	// List<HashMap<String, Object>> listDataResponse = (ArrayList<HashMap<String,
	// Object>>) apiResponse.get("data");
	// HashMap<String, Object> dataResponse = listDataResponse.get(0);
	//
	// company.setAccessTokenEndDate(apiResponse.get("access_token_end_date").toString());
	// company.setRefreshTokenEndDate(apiResponse.get("refresh_token_end_date").toString());
	//
	// company.setMainFunctionId(dataResponse.get("company_id").toString());
	// company.setPlatformId(dataResponse.get("company_ne_id").toString());
	// company.setLastAccessToken(accessToken);
	// company.setLastRefreshToken(refreshToken);
	//
	// logger.info("BaseCompany info ==============" + company.toString());
	// return companyRepository.createOrUpdate(company);
	// }

	/**
	 * Create and return User model base on information that obtained from the API
	 *
	 * @param companyId    the company that the user belongs to
	 * @param accessToken  current Access Token of Logged-In User
	 * @param refreshToken current Refresh Token of Logged-In User
	 * @return The User Object that contains user information
	 * @throws InstantiationException if has errors when the specified class object
	 *                                cannot be instantiated
	 * @throws IllegalAccessException if has errors when does not have access to the
	 *                                definition of the specified class
	 */
	// U createUser(long companyId, String accessToken, String refreshToken) throws
	// InstantiationException, IllegalAccessException{
	// U user = userClass.newInstance();
	// List<HashMap<String, Object>> listDataResponse = (ArrayList<HashMap<String,
	// Object>>) fetchUserInfo(accessToken, refreshToken).get("data");
	// HashMap<String, Object> dataResponse = listDataResponse.get(0);
	//
	// user.setCompanyId(companyId);
	// user.setUid(dataResponse.get("uid").toString());
	//
	// user.setAccessToken(accessToken);
	// user.setRefreshToken(refreshToken);
	//
	// logger.info("BaseUser info ==============" + user.toString());
	// return userRepository.createOrUpdate(user);
	// }

	/**
	 * Get Referrer Path
	 *
	 * @param request a HttpServletRequest object that contains the request the
	 *                client
	 * @return the path of the previous request
	 */
	public String getReferrerPath(HttpServletRequest request) {
		if (request.getParameter("path") != null) {
			return request.getParameter("path");
		}
		return Constant.ROOT_PATH;
	}

}
