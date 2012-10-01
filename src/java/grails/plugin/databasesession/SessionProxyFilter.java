package grails.plugin.databasesession;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Iterator;
import java.util.List;
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
	protected static final String SET_COOKIE = "Set-Cookie";

	private Persister persister;

	private Logger log = LoggerFactory.getLogger(getClass());

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

		chain.doFilter(requestWrapper, response);
	}

	protected HttpSession proxySession(final boolean create, final HttpServletRequest request,
			final HttpServletResponse response) {

		if (log.isDebugEnabled()) {
			log.debug("Proxying request for {}", request.getRequestURL());
		}

		String sessionId = getCookieValue(request, response);
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

	protected Cookie getCookie(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (COOKIE_NAME.equals(cookie.getName())) {
					return cookie;
				}
			}
		}

		// no cookie, but there might be a Set-Cookie header for a new cookie that hasn't made it to the request yet
		return parseSetCookieHeader(request, response);
	}

	protected Cookie parseSetCookieHeader(HttpServletRequest request, HttpServletResponse response) {
		String setCookie = response.getHeader(SET_COOKIE);
		if (setCookie == null) {
			return null;
		}

		log.trace("Found Set-Cookie header {}", setCookie);

		List<HttpCookie> parsedCookies = HttpCookie.parse(setCookie);
		for (Iterator<HttpCookie> iter = parsedCookies.listIterator(); iter.hasNext(); ) {
			HttpCookie c = iter.next();
			if (!COOKIE_NAME.equals(c.getName())) {
				iter.remove();
			}
		}

		if (parsedCookies.isEmpty()) {
			return null;
		}

		if (parsedCookies.size() > 1) {
			log.debug("Found multiple cookies in Set-Cookie header {}", setCookie);
			// TODO
		}

		HttpCookie parsedCookie = parsedCookies.get(0);
		if (request.getServerName().equals(parsedCookie.getDomain()) &&
				COOKIE_PATH.equals(parsedCookie.getPath()) &&
				request.isSecure() == parsedCookie.getSecure()) {
			log.debug("No Cookie, but found Set-Cookie header id {}", parsedCookie.getValue());
			return newCookie(parsedCookie.getValue(), request);
		}

		log.debug("Set-Cookie header mismatch: server {} vs {}, path {} vs {}, secure {} vs {}", new Object[] {
				request.getServerName(), parsedCookie.getDomain(),
				COOKIE_PATH, parsedCookie.getPath(),
				request.isSecure(), parsedCookie.getSecure() });
		return null;
	}

	protected String getCookieValue(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = getCookie(request, response);
		return cookie == null ? null : cookie.getValue();
	}

	protected void createCookie(String sessionId, HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = getCookie(request, response);
		if (cookie == null) {
			log.debug("Created new session cookie {}", sessionId);
		}
		else {
			log.debug("Updating existing cookie with id {} to new value {}", cookie.getValue(), sessionId);
		}
		cookie = newCookie(sessionId, request);
		response.addCookie(cookie);
	}

	protected Cookie newCookie(String sessionId, HttpServletRequest request) {
		Cookie cookie = new Cookie(COOKIE_NAME, sessionId);
		cookie.setDomain(request.getServerName()); // TODO needs config option
		cookie.setPath(COOKIE_PATH);
		cookie.setSecure(request.isSecure());
		return cookie;
	}

	protected void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = getCookie(request, response);
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

	@Override
	public void afterPropertiesSet() throws ServletException {
		super.afterPropertiesSet();
		Assert.notNull(persister, "persister must be specified");
	}
}
