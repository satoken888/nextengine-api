package jp.co.kawakyo.nextengineapi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Nextengine API SDK(http://api.next-e.jp/).
 *
 * @since 2013/11/01
 * @copyright Hamee Corp. All Rights Reserved.
 * @author Hamee Corp.
 */
public class NeApiClient {

    ////////////////////////////////////////////////////////////////////////////
    // 利用するサーバーのURLのスキーム＋ホスト名の定義
    ////////////////////////////////////////////////////////////////////////////
    public static final String SERVER_HOST_API = "https://api.next-engine.org";
    public static final String SERVER_HOST_NE = "https://base.next-engine.org";

    ////////////////////////////////////////////////////////////////////////////
    // 認証に用いるURLのパスを定義
    ////////////////////////////////////////////////////////////////////////////
    public static final String PATH_LOGIN = "/users/sign_in/"; // NEログイン
    public static final String PATH_OAUTH = "/api_neauth/"; // API認証

    ////////////////////////////////////////////////////////////////////////////
    // APIのレスポンスの処理結果ステータスの定義
    ////////////////////////////////////////////////////////////////////////////
    public static final String RESULT_SUCCESS = "success"; // 成功
    public static final String RESULT_ERROR = "error"; // 失敗
    public static final String RESULT_REDIRECT = "redirect"; // 要リダイレクト

    ////////////////////////////////////////////////////////////////////////////
    // Oauthパラメータの定義
    ////////////////////////////////////////////////////////////////////////////
    public static final String KEY_RESULT = "result"; // 結果
    public static final String KEY_CODE = "code"; // エラーコード
    public static final String KEY_MESSAGE = "message"; // メッセージ
    public static final String KEY_CLIENT_ID = "client_id"; // クライアントID
    public static final String KEY_CLIENT_SECRET = "client_secret";// クライアントシークレット
    public static final String KEY_REDIRECT_URI = "redirect_uri"; // リダイレクトURI
    public static final String KEY_UID = "uid"; // uid(ユーザー固有の識別ID)
    public static final String KEY_STATE = "state"; // state(有効期限)
    public static final String KEY_ACCESS_TOKEN = "access_token"; // アクセストークン
    public static final String KEY_REFRESH_TOKEN = "refresh_token";// リフレッシュトークン

    ///////////////////////////////////////////////////////
    // SDK内部でAPIを利用する為に使うメンバ変数
    ///////////////////////////////////////////////////////
    // OAuth認証のパラメータ
    protected String _client_id = null;
    protected String _client_secret = null;
    protected String _redirect_uri = null;
    protected String _uid = null;
    protected String _state = null;
    protected String _access_token = null;
    protected String _refresh_token = null;

    // リクエスト
    private HttpServletRequest _request = null;
    // レスポンス
    private HttpServletResponse _response = null;
    // Webアプリケーションか(false:バッチ等の非同期処理)
    private boolean _is_web = true;

    public String getAccessToken() {
        return _access_token;
    }

    public String getRefreshToken() {
        return _refresh_token;
    }

    protected String getServerHostApi() {
        return SERVER_HOST_API;
    }

    protected String getServerHostNe() {
        return SERVER_HOST_NE;
    }

    /**
     * 通常のWebアプリケーションの場合(同期でAPIを実行する場合)、本コンストラクタを呼びインスタンスを生成して下さい。
     *
     * redirect_uriの説明： まだ認証していないユーザーがアクセスした場合(ネクストエンジンログインが必要な場合)、
     * 本SDKが自動的にネクストエンジンのログイン画面にリダイレクトします（ユーザーには認証画面が表示される）。
     * ユーザーが認証した後、ネクストエンジンサーバーから認証情報と共にアプリケーションサーバーに
     * リダイレクトします。その際のアプリケーションサーバーのリダイレクト先uriです。
     *
     * @param request       リクエスト
     * @param response      レスポンス
     * @param client_id     クライアントID
     * @param client_secret クライアントシークレット
     * @param redirect_uri  ネクストエンジンログインが必要な場合、 一度ログイン画面に遷移します。ログイン後の
     *                      アプリケーションサーバーのリダイレクト先を指定します。
     * @throws NeApiClientException SDKの仕様方法が間違っている
     */
    public NeApiClient(HttpServletRequest request, HttpServletResponse response, String client_id, String client_secret,
            String redirect_uri) throws NeApiClientException {
        this._is_web = true;

        this._request = request;
        if (!(this._request instanceof HttpServletRequest)) {
            throw new NeApiClientException("requestにはHttpServletRequestを指定して下さい。");
        }

        this._response = response;
        if (!(this._response instanceof HttpServletResponse)) {
            throw new NeApiClientException("responseにはHttpServletResponseを指定して下さい。");
        }

        this._client_id = client_id;
        if (this._client_id == null) {
            throw new NeApiClientException("client_idはアプリを作る->APIタブのクライアントIDを指定して下さい。");
        }

        this._client_secret = client_secret;
        if (this._client_secret == null) {
            throw new NeApiClientException("client_secretはアプリを作る->APIタブのクライアントシークレットを指定して下さい。");
        }

        this._redirect_uri = redirect_uri;
        if (this._redirect_uri == null) {
            throw new NeApiClientException("redirect_uriはユーザーがログインした後に、リダイレクトされるアプリケーションサーバーのURIを指定して下さい。");
        }
    }

    /**
     * 一度認証した後、バッチ等非同期で、前回の接続情報から再度接続する場合は本メソッドを使います。
     * 
     * access_tokenとrefresh_tokenの説明：
     * 指定する値は、最後にapiExecute又はneLogin呼び出した後のgetAccessToken(),getRefreshToken()の戻り値です。
     * 注意：この値はユーザー毎(uid毎)に管理する必要があります。別のユーザーの値を指定してSDKを実行すると
     * 他ユーザーの情報にアクセスしてしまうため、厳重にご注意をお願いします。
     *
     * @param access_token  NE APIによって発行されたアクセストークン
     * @param refresh_token NE APIによって発行されたリフレッシュトークン
     * @throws NeApiClientException SDKの仕様方法が間違っている
     */
    public NeApiClient(String access_token, String refresh_token) throws NeApiClientException {
        this._is_web = false;

        this._access_token = access_token;
        if (this._access_token == null) {
            throw new NeApiClientException(
                    "access_tokenは最後にapiExecute又はneLogin呼び出した後のgetAccessToken(),getRefreshToken()の戻り値を設定して下さい。");
        }

        this._refresh_token = refresh_token;
    }

    /**
     * ネクストエンジンログインのみ実行します。 既にログインしている場合、ログイン後の基本情報を返却します。
     * まだログインしていない場合、ネクストエンジンログイン画面にリダイレクトされ、
     * 正しくログインした場合、redirect_uriにリダイレクトされます。
     * リダイレクト先で、再度neLoginを呼ぶ事で、ログインしたユーザーの基本情報を返却します。
     * （redirect_uriをインスタンス生成後から変更したい場合用）。
     *
     * @param redirect_uri ユーザーがネクストエンジンログインをした後のアプリケーションサーバーのURI
     * @return ログインしたユーザーの基本情報
     * @throws IOException          入出力関係
     * @throws NeApiClientException SDKの仕様方法が間違っている
     */
    public HashMap<String, Object> neLogin(String redirect_uri) throws IOException, NeApiClientException {
        this._redirect_uri = redirect_uri;
        return this.neLogin();
    }

    /**
     * ネクストエンジンログインのみ実行します。 既にログインしている場合、ログイン後の基本情報を返却します。
     * まだログインしていない場合、ネクストエンジンログイン画面にリダイレクトされ、
     * 正しくログインした場合、redirect_uriにリダイレクトされます。
     * リダイレクト先で、再度neLoginを呼ぶ事で、ログインしたユーザーの基本情報を返却します。
     * （redirect_uriをインスタンス生成後から変更しない場合用）。
     *
     * @return ログインしたユーザーの基本情報
     * @throws IOException          入出力関係
     * @throws NeApiClientException SDKの仕様方法が間違っている
     */
    public HashMap<String, Object> neLogin() throws IOException, NeApiClientException {
        HashMap<String, Object> response;

        if (!this._is_web) {
            throw new NeApiClientException("本メソッドは、第一引数がHttpServletRequestのコンスラクタで初期化する必要があります。");
        }

        // メンバ変数にuid及びstateを設定
        setUidAndState();
        if (this._response.isCommitted()) {
            return null;
        }

        // APIサーバーの認証を実施
        response = this.AuthApi();

        // リダイレクトが必要ならリダイレクトする
        this.responseCheck(response);

        return response;
    }

    /**
     * ネクストエンジンAPIを実行し、結果を返します。
     * 通常のWebアプリケーションからの実行(第一引数がHttpServletRequestのコンストラクタ)の場合 ネクストエンジンに認証します。
     * 既に認証済みでバッチ等非同期処理で実施する場合は、access_tokenでAPIを実施します。
     * （redirect_uriをインスタンス生成後から変更したい場合・パラメータがあるAPIの場合用）。
     *
     * @param path         実行するAPIのURLのホスト名以降のパス(Ex:/api_v1_master_stock/search)。
     * @param api_params   実行するAPIの入力パラメータ。不要な場合は省略して下さい。
     * @param redirect_uri ユーザーがネクストエンジンログインをした後のアプリケーションサーバーのURI
     * @return APIの実行結果（出力パラメータ）
     * @throws IOException          入出力関係
     * @throws NeApiClientException SDKの仕様方法が間違っている（path）等
     */
    public HashMap<String, Object> apiExecute(String path, HashMap<String, String> api_params, String redirect_uri)
            throws IOException, NeApiClientException {
        this._redirect_uri = redirect_uri;
        return this.apiExecute(path, api_params);
    }

    /**
     * ネクストエンジンAPIを実行し、結果を返します。
     * 通常のWebアプリケーションからの実行(第一引数がHttpServletRequestのコンストラクタ)の場合 ネクストエンジンに認証します。
     * 既に認証済みでバッチ等非同期処理で実施する場合は、access_tokenでAPIを実施します。
     * （redirect_uriをインスタンス生成後から変更しない場合・パラメータがあるAPIの場合用）。
     *
     * @param path       実行するAPIのURLのホスト名以降のパス(Ex:/api_v1_master_stock/search)。
     * @param api_params 実行するAPIの入力パラメータ。不要な場合は省略して下さい。
     * @return APIの実行結果（出力パラメータ）
     * @throws IOException          入出力関係
     * @throws NeApiClientException SDKの仕様方法が間違っている（path）等
     */
    public HashMap<String, Object> apiExecute(String path, HashMap<String, String> api_params)
            throws IOException, NeApiClientException {
        HashMap<String, Object> response;

        if (this._is_web) {
            // メンバ変数にuid及びstateを設定
            setUidAndState();
            if (this._response.isCommitted()) {
                return null;
            }

            // まだアクセストークンを取得していない場合のみ
            if (this._access_token == null) {
                // APIサーバーの認証を実施
                response = this.AuthApi();
                if (!responseCheck(response)) {
                    return response;
                }
            }
        }

        @SuppressWarnings("unchecked")
        HashMap<String, String> params = (HashMap<String, String>) api_params.clone();
        params.put(KEY_ACCESS_TOKEN, this._access_token);
        if (this._refresh_token != null) {
            params.put(KEY_REFRESH_TOKEN, this._refresh_token);
        }
        response = this.post(getServerHostApi() + path, params);

        // リダイレクトが必要ならリダイレクトする
        this.responseCheck(response);

        return response;
    }

    /**
     * ネクストエンジンAPIを実行し、結果を返します。
     * 通常のWebアプリケーションからの実行(第一引数がHttpServletRequestのコンストラクタ)の場合 ネクストエンジンに認証します。
     * 既に認証済みでバッチ等非同期処理で実施する場合は、access_tokenでAPIを実施します。
     * （redirect_uriをインスタンス生成後から変更したい場合・パラメータがないAPIの場合用）。
     *
     * @param path         実行するAPIのURLのホスト名以降のパス(Ex:/api_v1_master_stock/search)。
     * @param redirect_uri ユーザーがネクストエンジンログインをした後のアプリケーションサーバーのURI
     * @return APIの実行結果（出力パラメータ）
     * @throws IOException          入出力関係
     * @throws NeApiClientException SDKの仕様方法が間違っている（path）等
     */
    public HashMap<String, Object> apiExecute(String path, String redirect_uri)
            throws IOException, NeApiClientException {
        this._redirect_uri = redirect_uri;
        return apiExecute(path);
    }

    /**
     * ネクストエンジンAPIを実行し、結果を返します。
     * 通常のWebアプリケーションからの実行(第一引数がHttpServletRequestのコンストラクタ)の場合 ネクストエンジンに認証します。
     * 既に認証済みでバッチ等非同期処理で実施する場合は、access_tokenでAPIを実施します。
     * （redirect_uriをインスタンス生成後から変更しない場合・パラメータがないAPIの場合用）。
     *
     * @param path 実行するAPIのURLのホスト名以降のパス(Ex:/api_v1_master_stock/search)。
     * @return APIの実行結果（出力パラメータ）
     * @throws IOException          入出力関係
     * @throws NeApiClientException SDKの仕様方法が間違っている（path）等
     */
    public HashMap<String, Object> apiExecute(String path) throws IOException, NeApiClientException {
        HashMap<String, String> api_params = new HashMap<>();
        return apiExecute(path, api_params);
    }

    /**
     * ネクストエンジンログインが不要なネクストエンジンAPIを実行し、結果を返します。
     *
     * @param path 実行するAPIのURLのホスト名以降のパス(Ex:/api_app/company)。
     * @return APIの実行結果（出力パラメータ）
     * @throws IOException 入出力関係
     */
    public HashMap<String, Object> apiExecuteNoRequiredLogin(String path) throws IOException {
        HashMap<String, String> api_params = new HashMap<>();
        return this.apiExecuteNoRequiredLogin(path, api_params);
    }

    /**
     * ネクストエンジンログインが不要なネクストエンジンAPIを実行し、結果を返します。
     *
     * @param path       実行するAPIのURLのホスト名以降のパス(Ex:/api_app/company)。
     * @param api_params 実行するAPIの入力パラメータ。不要な場合は省略して下さい。
     * @return APIの実行結果（出力パラメータ）
     * @throws IOException 入出力関係
     */
    public HashMap<String, Object> apiExecuteNoRequiredLogin(String path, HashMap<String, String> api_params)
            throws IOException {
        api_params.put(KEY_CLIENT_ID, this._client_id);
        api_params.put(KEY_CLIENT_SECRET, this._client_secret);

        return this.post(getServerHostApi() + path, api_params);
    }

    protected void setUidAndState() throws IOException {
        this._uid = this._request.getParameter(KEY_UID);
        this._state = this._request.getParameter(KEY_STATE);

        // uid又はstateがないならNEにログイン
        if (this._uid == null || this._state == null) {
            redirectNeLogin();
        }
    }

    protected HashMap<String, Object> AuthApi() throws IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put(KEY_UID, this._uid);
        params.put(KEY_STATE, this._state);
        params.put(KEY_CLIENT_ID, this._client_id);
        params.put(KEY_CLIENT_SECRET, this._client_secret);

        return this.post(getServerHostApi() + PATH_OAUTH, params);
    }

    protected void redirectNeLogin() throws IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put(KEY_CLIENT_ID, this._client_id);
        if (this._redirect_uri != null) {
            params.put(KEY_REDIRECT_URI, this._redirect_uri);
        }

        String url = getServerHostNe() + PATH_LOGIN + "?" + getUrlParams(params);
        this._response.sendRedirect(url);
    }

    protected boolean responseCheck(HashMap<String, Object> response) throws IOException, NeApiClientException {
        String result = (String) response.get(KEY_RESULT);
        String access_token = (String) response.get(KEY_ACCESS_TOKEN);
        String refresh_token = (String) response.get(KEY_REFRESH_TOKEN);

        if (result == null)
            throw new NeApiClientException("クライアントID・シークレットや指定したパスが正しいか確認して下さい。");
        if (access_token != null)
            this._access_token = access_token;
        if (refresh_token != null)
            this._refresh_token = refresh_token;

        // 正常終了
        if (result.equals(RESULT_SUCCESS))
            return true;

        // リダイレクトの場合
        if (result.equals(RESULT_REDIRECT)) {
            // Webアプリケーションからの実行の場合、リダイレクトを実施
            if (this._is_web) {
                this.redirectNeLogin();
            }
            return false;
        }
        // エラーの場合
        return false;
    }

    protected HashMap<String, Object> post(String url_str, HashMap<String, String> params) throws IOException {
        // アドレス設定、ヘッダー情報設定
        URL url = new URL(url_str);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setUseCaches(false);
        if (this._redirect_uri != null) {
            con.setRequestProperty("Referer", "https://" + new URL(this._redirect_uri).getHost());
        }
        PrintWriter pw = new PrintWriter(con.getOutputStream());
        pw.print(getUrlParams(params));
        pw.close();

        InputStream is = con.getInputStream();

        return new ObjectMapper().readValue(is, new TypeReference<HashMap<String, Object>>() {
        });
    }

    protected static String getUrlParams(HashMap<String, String> params) {
        return urlEncodeUTF8(params);
    }

    protected static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    protected static String urlEncodeUTF8(HashMap<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    urlEncodeUTF8(entry.getKey().toString()),
                    urlEncodeUTF8(entry.getValue().toString())));
        }
        return sb.toString();
    }

}