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

	private final Persister _persister;
	private final String _sessionId;
	private final long _creationTime = System.currentTimeMillis();
	private final ServletContext _servletContext;

	private static final HttpSessionContext SESSION_CONTEXT = new HttpSessionContext() {
		public HttpSession getSession(String sessionId) {
			return null;
		}
		public Enumeration<String> getIds() {
			return SESSION_CONTEXT_ID_ENUM;
		}
	};

	private static final Enumeration<String> SESSION_CONTEXT_ID_ENUM = new Enumeration<String>() {
		public String nextElement() {
			return null;
		}
		public boolean hasMoreElements() {
			return false;
		}
	};

	/**
	 * Constructor.
	 * @param servletContext the ServletContext
	 * @param persister the persister
	 * @param sessionId session id
	 */
	public SessionProxy(final ServletContext servletContext, final Persister persister, String sessionId) {
		_servletContext = servletContext;
		_persister = persister;
		_sessionId = sessionId;
	}

	public Object getAttribute(String name) {
		try {
			return _persister.getAttribute(_sessionId, name);
		}
		catch (InvalidatedSessionException e) {
			invalidate();
			throw new IllegalStateException("Session already invalidated");
		}
	}

	public Object getValue(String name) {
		return getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() {

		try {
			final Iterator<String> iterator = _persister.getAttributeNames(_sessionId).iterator();
			return new Enumeration<String>() {
				public boolean hasMoreElements() {
					return iterator.hasNext();
				}
				public String nextElement() {
					return iterator.next();
				}
			};
		}
		catch (InvalidatedSessionException e) {
			invalidate();
			throw new IllegalStateException("Session already invalidated");
		}
	}

	public String[] getValueNames() {
		try {
			List<String> names = _persister.getAttributeNames(_sessionId);
			return names.toArray(new String[names.size()]);
		}
		catch (InvalidatedSessionException e) {
			invalidate();
			throw new IllegalStateException("Session already invalidated");
		}
	}

	public void setAttribute(String name, Object value) {
		try {
			_persister.setAttribute(_sessionId, name, value);
		}
		catch (InvalidatedSessionException e) {
			invalidate();
			throw new IllegalStateException("Session already invalidated");
		}
	}

	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	public void removeAttribute(String name) {
		try {
			_persister.removeAttribute(_sessionId, name);
		}
		catch (InvalidatedSessionException e) {
			invalidate();
			throw new IllegalStateException("Session already invalidated");
		}
	}

	public void removeValue(String name) {
		removeAttribute(name);
	}

	public long getCreationTime() {
		return _creationTime;
	}

	public String getId() {
		return _sessionId;
	}

	public long getLastAccessedTime() {
		try {
			return _persister.getLastAccessedTime(_sessionId);
		}
		catch (InvalidatedSessionException e) {
			invalidate();
			throw new IllegalStateException("Session already invalidated");
		}
	}

	public ServletContext getServletContext() {
		return _servletContext;
	}

	public void setMaxInactiveInterval(int interval) {
		try {
			_persister.setMaxInactiveInterval(_sessionId, interval);
		}
		catch (InvalidatedSessionException e) {
			invalidate();
			throw new IllegalStateException("Session already invalidated");
		}
	}

	public int getMaxInactiveInterval() {
		try {
			return _persister.getMaxInactiveInterval(_sessionId);
		}
		catch (InvalidatedSessionException e) {
			invalidate();
			throw new IllegalStateException("Session already invalidated");
		}
	}

	public HttpSessionContext getSessionContext() {
		return SESSION_CONTEXT;
	}

	public void invalidate() {
		_persister.invalidate(_sessionId);
	}

	public boolean isNew() {
		return false; // TODO
	}
}
