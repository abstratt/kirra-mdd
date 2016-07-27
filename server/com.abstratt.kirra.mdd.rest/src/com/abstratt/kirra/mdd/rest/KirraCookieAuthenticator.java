package com.abstratt.kirra.mdd.rest;

import java.util.Base64;
import java.util.StringTokenizer;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CookieSetting;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.ext.crypto.CookieAuthenticator;
import org.restlet.util.Series;

public class KirraCookieAuthenticator extends CookieAuthenticator implements KirraAuthenticationContext {

	public KirraCookieAuthenticator(Restlet toMonitor) {
        super(null, "Cloudfier App", "u7YzXaKLlsq+KJ1z".getBytes());
        setVerifier(new KirraSecretVerifier());
        setMultiAuthenticating(true);
        setNext(toMonitor);
        setIdentifierFormName("username");
    }
    
    @Override
    public String getCookieName() {
        return "cloudfier-" + WORKSPACE_NAME.get() + "-credentials";
    }

    @Override
    public String getLoginPath() {
        return "/" + WORKSPACE_NAME.get() + "/session/login";
    }

    @Override
    public String getLogoutPath() {
        return "/" + WORKSPACE_NAME.get() + "/session/logout";
    }
    
    @Override
    public String getRealm() {
        return WORKSPACE_NAME.get() + "-realm";
    }
    
    @Override
    protected CookieSetting getCredentialsCookie(Request request, Response response) {
    	CookieSetting credentialsCookie = super.getCredentialsCookie(request, response);
    	String cookiePath = request.getRootRef().getPath() +'/' + WORKSPACE_NAME.get();
    	credentialsCookie.setPath(cookiePath);
		return credentialsCookie;
    }
    
    @Override
    protected void login(Request request, Response response) {
    	Series<Header> requestHeaders = (Series<Header>)request.getAttributes().computeIfAbsent("org.restlet.http.headers", key -> new Series<Header>(Header.class));
    	Header authorizationHeader = requestHeaders.getFirst("Authorization");
		if (authorizationHeader == null || !authorizationHeader.getValue().startsWith("Custom ")) {
			super.logout(request, response);
			return;
		}
		String authorization = authorizationHeader.getValue();
		
		String encodedUserPassword = authorization.replaceFirst("Custom ", "");
		String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword));
		StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
		String username = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
		String password = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;

        // Set credentials
        ChallengeResponse cr = new ChallengeResponse(new ChallengeScheme("Custom", "custom"), username, password);
        request.setChallengeResponse(cr);

        // Attempt to redirect
        attemptRedirect(request, response);
    }

    @Override
    protected int beforeHandle(final Request request, final Response response) {
    	configure(request);
        return super.beforeHandle(request, response);
    }
    
    @Override
    protected boolean isLoggingIn(Request request, Response response) {
        return isInterceptingLogin() && Method.POST.equals(request.getMethod()) && request.getResourceRef().toString().endsWith("/login");
    }

    @Override
    protected boolean isLoggingOut(Request request, Response response) {
        return isInterceptingLogout() && (Method.GET.equals(request.getMethod()) || Method.POST.equals(request.getMethod()))
                        && request.getResourceRef().toString().endsWith("/logout");
    }

    @Override
    protected int unauthenticated(Request request, Response response) {
    	super.unauthenticated(request, response);
    	return LOGIN_REQUIRED.get() ? STOP : CONTINUE; 
    }
    @Override
    public boolean isOptional() {
    	return KirraAuthenticationContext.super.isOptional() || !IS_AJAX.get();
    }
}