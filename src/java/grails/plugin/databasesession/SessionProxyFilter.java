package grails.plugin.databasesession;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;

/**
 * @author Burt Beckwith
 */
public class SessionProxyFilter extends GenericFilterBean {

	private Persister persister;

	public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
			throws IOException, ServletException {

		final HttpServletRequest request = (HttpServletRequest)req;

		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {

			@Override
			public HttpSession getSession(boolean create) {
				HttpSession session = request.getSession(create);
				return session == null ? null : new SessionProxy(session, persister);
			}

			@Override
			public HttpSession getSession() {
				return getSession(true);
			}
		};

		chain.doFilter(wrapper, res);
	}

	/**
	 * Dependency injection for the persister.
	 * @param persister the persister
	 */
	public void setPersister(Persister persister) {
		this.persister = persister;
	}

	@Override
	public void afterPropertiesSet() throws ServletException {
		super.afterPropertiesSet();
		Assert.notNull(persister, "persister must be specified");
	}
}
