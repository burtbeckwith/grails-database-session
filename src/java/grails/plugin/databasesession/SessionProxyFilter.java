package grails.plugin.databasesession;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Registers a request wrapper that intercepts getSession() calls and returns a
 * database-backed implementation.
 *
 * @author Burt Beckwith
 */
public class SessionProxyFilter extends OncePerRequestFilter {

	protected static final String COOKIE_NAME = "SessionProxyFilter_SessionId";
	protected static final String COOKIE_PATH = "/";
	protected static final String REQUEST_COOKIE_KEY = "SessionProxyFilter_REQUEST_COOKIE";

	private static final ThreadLocal<HttpServletRequest> REQUEST_HOLDER = new ThreadLocal<HttpServletRequest>();
	private static final ThreadLocal<HttpServletResponse> RESPONSE_HOLDER = new ThreadLocal<HttpServletResponse>();

	private Persister persister;

	protected Logger log = LoggerFactory.getLogger(getClass());

	@Override
	protected void doFilterInternal(final HttpServletRequest request,
			final HttpServletResponse response, final FilterChain chain)
					throws ServletException, IOException {

		HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(request) {

			@Override
			public HttpSession getSession(boolean create) {
				return proxySession(create, request, response);
			}

			@Override
			public HttpSession getSession() {
				return getSession(true);
			}
		};

		REQUEST_HOLDER.set(requestWrapper);
		RESPONSE_HOLDER.set(response);

		try {
			chain.doFilter(requestWrapper, response);
		}
		finally {
			REQUEST_HOLDER.remove();
			RESPONSE_HOLDER.remove();
		}
	}

	protected HttpSession proxySession(final boolean create, final HttpServletRequest request,
			final HttpServletResponse response) {

		if (log.isDebugEnabled()) {
			log.debug("Proxying request for {}", request.getRequestURL());
		}

		String sessionId = getCookieValue(request);
		if (sessionId == null) {
			if (!create) {
				log.debug("No session cookie but create is false, not creating session");
				// no session cookie but don't create
				return null;
			}

			// no session cookie but do create
			log.debug("No session cookie but create is true, creating session");
			sessionId = createSession(request, response);
			return new SessionProxy(getServletContext(), persister, sessionId);
		}

		if (persister.isValid(sessionId)) {
			// session cookie and the session is still active
			log.debug("Session cookie {} found", sessionId);
			return new SessionProxy(getServletContext(), persister, sessionId);
		}

		if (!create) {
			// session cookie but it's been invalidated or is too old, but don't create
			log.debug("Session cookie {} found but invalid or old and create is false; not creating session", sessionId);
			deleteCookie(request, response);
			persister.invalidate(sessionId); // cleanup if it's too old
			return null;
		}

		// session cookie but it's been invalidated or is too old
		log.debug("Session cookie {} found but invalid or old and create is true, creating session", sessionId);
		persister.invalidate(sessionId); // cleanup if it's too old
		sessionId = createSession(request, response);
		return new SessionProxy(getServletContext(), persister, sessionId);
	}

	protected String createSession(final HttpServletRequest request, final HttpServletResponse response) {
		String sessionId = generateSessionId();
		persister.create(sessionId);
		log.debug("Created new session {} for URL {}", new Object[] { sessionId, request.getRequestURL() });
		createCookie(sessionId, request, response);
		return sessionId;
	}

	protected String generateSessionId() {
		return UUID.randomUUID().toString();
	}

	protected Cookie getCookie(HttpServletRequest request) {
		// no cookie, but if we're in the same request as when it was set it will be here
		Cookie newCookie = (Cookie)request.getAttribute(REQUEST_COOKIE_KEY);
		if (newCookie != null) {
			return newCookie;
		}

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (COOKIE_NAME.equals(cookie.getName())) {
					return cookie;
				}
			}
		}

		return null;
	}

	protected String getCookieValue(HttpServletRequest request) {
		Cookie cookie = getCookie(request);
		return cookie == null ? null : cookie.getValue();
	}

	protected void createCookie(String sessionId, HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = getCookie(request);
		if (cookie == null) {
			log.debug("Created new session cookie {}", sessionId);
		}
		else {
			log.debug("Updating existing cookie with id {} to new value {}", cookie.getValue(), sessionId);
		}
		cookie = newCookie(sessionId, request);
		response.addCookie(cookie);
		request.setAttribute(REQUEST_COOKIE_KEY, cookie);
	}

	protected Cookie newCookie(String sessionId, HttpServletRequest request) {
		Cookie cookie = new Cookie(COOKIE_NAME, sessionId);
		cookie.setDomain(request.getServerName()); // TODO needs config option
		cookie.setPath(COOKIE_PATH);
		cookie.setSecure(request.isSecure());
		return cookie;
	}

	protected void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = getCookie(request);
		if (cookie == null) {
			return;
		}

		cookie = newCookie(cookie.getValue(), request);
		cookie.setMaxAge(0);
		response.addCookie(cookie);
		log.debug("Deleted cookie with id {}", cookie.getValue());
	}

	/**
	 * Dependency injection for the persister.
	 * @param persister the persister
	 */
	public void setPersister(Persister persister) {
		this.persister = persister;
	}

	protected Persister getPersister() {
		return persister;
	}

	/**
	 * Get the current request.
	 * @return the request
	 */
	public static HttpServletRequest getRequest() {
		return REQUEST_HOLDER.get();
	}

	/**
	 * Get the current response.
	 * @return the response
	 */
	public static HttpServletResponse getResponse() {
		return RESPONSE_HOLDER.get();
	}

	@Override
	public void afterPropertiesSet() throws ServletException {
		super.afterPropertiesSet();
		Assert.notNull(persister, "persister must be specified");
	}
}
