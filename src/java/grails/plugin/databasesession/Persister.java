package grails.plugin.databasesession;

import java.util.List;

/**
 * @author Burt Beckwith
 */
public interface Persister {

	/**
	 * Retrieve an attribute value.
	 * @param sessionId the session id
	 * @param name the attribute name
	 * @param lastAccessedTime the last accessed time from the real session
	 * @return the value or null
	 */
	Object getAttribute(String sessionId, String name, long lastAccessedTime);

	/**
	 * Store an attribute value.
	 * @param sessionId the session id
	 * @param name the attribute name
	 * @param value the value
	 * @param lastAccessedTime the last accessed time from the real session
	 */
	void setAttribute(String sessionId, String name, Object value, long lastAccessedTime);

	/**
	 * Delete a persistent attribute value.
	 * @param sessionId the session id
	 * @param name the attribute name
	 * @param lastAccessedTime the last accessed time from the real session
	 */
	void removeAttribute(String sessionId, String name, long lastAccessedTime);

	/**
	 * Get all attribute names for the session.
	 * @param sessionId the session id
	 * @return the names (never null, may be empty)
	 */
	List<String> getAttributeNames(String sessionId);

	/**
	 * Delete a session and its attributes.
	 * @param sessionId the session id
	 */
	void invalidate(String sessionId);

	/**
	 * Register a new persistent session.
	 * @param sessionId the session id
	 * @param creationTime the creation time
	 */
	void create(String sessionId, long creationTime);
}
