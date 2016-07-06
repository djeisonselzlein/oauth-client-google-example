package br.com.selzleind.oauthClientGoogle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

public class App {

	private static final String BASE_URL = "http://204.197.244.106/~magentows4aw/loja/";
	private static final String INITIATE = BASE_URL + "oauth/initiate";
	private static final String AUTHORIZE = BASE_URL + "admin/oauth_authorize";
	private static final String TOKEN = BASE_URL + "oauth/token";
	private static final String MAGENTO_REST_API_URL = BASE_URL + "api/rest";

	public static void main(String[] args) {
		final String MAGENTO_API_KEY = "oauth_consumer_key";
		final String MAGENTO_API_SECRET = "oauth_consumer_secret";

		HttpTransport transport = new NetHttpTransport();

		//
		try {

			// The OAuthHmasSigner will be used to create the oauth_signature
			// Using HMAC-SHAl as the oauth_signature_method
			// The signer needs the secret key to sign requests
			OAuthHmacSigner signer = new OAuthHmacSigner();
			signer.clientSharedSecret = MAGENTO_API_SECRET;

			// Step 1: Get a request token
			// -------------------------------
			// We need to provide our application key
			// We also need to provide an HTTP transport object
			// And the signer which will sign the request
			OAuthGetTemporaryToken temporaryToken = new OAuthGetTemporaryToken(INITIATE);
			temporaryToken.callback = "http://localhost:8080";
			temporaryToken.consumerKey = MAGENTO_API_KEY;
			temporaryToken.transport = transport;
			temporaryToken.signer = signer;

			// Get back our request token
			OAuthCredentialsResponse requestTokenResponse = temporaryToken.execute();

			System.out.println("Request Token: ");
			System.out.println("- oauth_token    = " + requestTokenResponse.token);
			System.out.println("-oauth_token_secret = " + requestTokenResponse.tokenSecret);

			// Update the signer to also include the request token
			signer.tokenSharedSecret = requestTokenResponse.tokenSecret;

			// Step 2: User grants access
			// ----------------------------------------------

			// Construct an authorization URL using the temporary request token
			OAuthAuthorizeTemporaryTokenUrl authorizeTemporaryTokenUrl = new OAuthAuthorizeTemporaryTokenUrl(AUTHORIZE);
			authorizeTemporaryTokenUrl.temporaryToken = requestTokenResponse.token;

			// We ask the user to open this URL and grant access
			// Magento includes an extra safety measure, asks the user to
			// provide PIN
			String pin = null;
			System.out.println("Go to the following link:\n" + authorizeTemporaryTokenUrl.build());
			InputStreamReader converter = new InputStreamReader(System.in, "UTF-8");
			BufferedReader in = new BufferedReader(converter);
			while (pin == null) {
				System.out.println("Enter the verification PIN provided by Magento:");
				pin = in.readLine();
			}

			// step 3: Request the access token the user has approved
			// ---------------------------------------------------------------

			// Get the access token
			// We need to provide our application key
			// The signer, the transport objects
			// The temporary request token
			// And a verifier string ( the PIN number provided by Twitter)
			OAuthGetAccessToken accessToken = new OAuthGetAccessToken(TOKEN);
			accessToken.consumerKey = MAGENTO_API_KEY;
			accessToken.signer = signer;
			accessToken.transport = transport;
			accessToken.temporaryToken = requestTokenResponse.token;
			accessToken.verifier = pin;

			// Get back our access token
			OAuthCredentialsResponse accessTokenResponse = accessToken.execute();

			System.out.println("Access Token: ");
			System.out.println("-oauth_token = " + accessTokenResponse.token);
			System.out.println("-oauth_token_secret = " + accessTokenResponse.tokenSecret);

			// Update the signer again
			// We now replace the temporary request token with the final access
			// token
			signer.tokenSharedSecret = accessTokenResponse.tokenSecret;

			// Set up OAuth parameters which can now be used in authenticated
			// requests
			OAuthParameters parameters = new OAuthParameters();
			parameters.consumerKey = MAGENTO_API_KEY;
			parameters.token = accessTokenResponse.token;
			parameters.signer = signer;

			// OAuth steps finished, we can now start accessing the service
			// --------------------------------------------------------------
			HttpRequestFactory factory = transport.createRequestFactory(parameters);
			GenericUrl url = new GenericUrl(MAGENTO_REST_API_URL + "/products?limit=2");
			HttpRequest req = factory.buildGetRequest(url);
			HttpResponse resp = req.execute();

			System.out.println(resp.getStatusCode());
			System.out.println(resp.parseAsString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
