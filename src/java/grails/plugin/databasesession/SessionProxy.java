package grails.plugin.databasesession;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * @author Burt Beckwith
 */
@SuppressWarnings("deprecation")
public class SessionProxy implements HttpSession {

	private final HttpSession _session;
	private final Persister _persister;

	/**
	 * Constructor.
	 * @param session the real session
	 * @param persister the persister
	 */
	public SessionProxy(final HttpSession session, final Persister persister) {
		_session = session;
		_persister = persister;
		_persister.create(session.getId(), session.getCreationTime());
	}

	public Object getAttribute(String name) {
		return _persister.getAttribute(_session.getId(), name, _session.getLastAccessedTime());
	}

	public Object getValue(String name) {
		return getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() {

		final Iterator<String> iterator = _persister.getAttributeNames(_session.getId()).iterator();

		return new Enumeration<String>() {
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}
			public String nextElement() {
				return iterator.next();
			}
		};
	}

	public String[] getValueNames() {
		List<String> names = _persister.getAttributeNames(_session.getId());
		return names.toArray(new String[names.size()]);
	}

	public void setAttribute(String name, Object value) {
		_persister.setAttribute(_session.getId(), name, value, _session.getLastAccessedTime());
	}

	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	public void removeAttribute(String name) {
		_persister.removeAttribute(_session.getId(), name, _session.getLastAccessedTime());
	}

	public void removeValue(String name) {
		removeAttribute(name);
	}

	public long getCreationTime() {
		return _session.getCreationTime();
	}

	public String getId() {
		return _session.getId();
	}

	public long getLastAccessedTime() {
		return _session.getLastAccessedTime();
	}

	public ServletContext getServletContext() {
		return _session.getServletContext();
	}

	public void setMaxInactiveInterval(int interval) {
		_session.setMaxInactiveInterval(interval);
	}

	public int getMaxInactiveInterval() {
		return _session.getMaxInactiveInterval();
	}

	public HttpSessionContext getSessionContext() {
		return _session.getSessionContext();
	}

	public void invalidate() {
		_persister.invalidate(_session.getId());
		_session.invalidate();
	}

	public boolean isNew() {
		return _session.isNew();
	}
}
