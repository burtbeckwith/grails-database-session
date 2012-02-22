package grails.plugin.databasesession

import grails.util.GrailsUtil
import grails.validation.ValidationException

/**
 * @author Burt Beckwith
 */
class GormPersisterService implements Persister {

	def grailsApplication

	void create(String sessionId) {
		try {
			if (PersistentSession.exists(sessionId)) {
				return
			}

			PersistentSession session = new PersistentSession()
			session.creationTime = System.currentTimeMillis()
			session.lastAccessedTime = session.creationTime
			session.id = sessionId
			session.save(failOnError: true)
		}
		catch (e) {
			handleException e
		}
	}

	Object getAttribute(String sessionId, String name) throws InvalidatedSessionException {
		if (name == null) return null

		try {
			PersistentSession session = PersistentSession.get(sessionId)
			checkInvalidated session
			session.lastAccessedTime = System.currentTimeMillis()

			PersistentSessionAttributeValue.findBySessionAndAttributeName(session, name)?.value
		}
		catch (e) {
			handleException e
		}
	}

	void setAttribute(String sessionId, String name, value) throws InvalidatedSessionException {

		if (name == null) {
			throw new IllegalArgumentException('name parameter cannot be null')
		}

		if (value == null) {
			removeAttribute sessionId, name
			return
		}

		try {
			PersistentSession session = PersistentSession.get(sessionId)
			checkInvalidated session
			session.lastAccessedTime = System.currentTimeMillis()

			def attr = PersistentSessionAttribute.findBySessionAndName(session, name)

			PersistentSessionAttributeValue psav
			if (attr) {
				psav = PersistentSessionAttributeValue.findByAttribute(attr)
			}
			else {
				attr = new PersistentSessionAttribute()
				attr.session = session
				attr.name = name
				attr.save(failOnError: true)
				psav = new PersistentSessionAttributeValue()
				psav.attribute = attr
			}

			psav.value = value
			psav.save(failOnError: true)
		}
		catch (e) {
			handleException e
		}
	}

	void removeAttribute(String sessionId, String name) throws InvalidatedSessionException {
		if (name == null) return

		try {
			PersistentSession session = PersistentSession.get(sessionId)
			checkInvalidated session
			session.lastAccessedTime = System.currentTimeMillis()

			PersistentSessionAttributeValue.remove sessionId, name
			PersistentSessionAttribute.remove sessionId, name
		}
		catch (e) {
			handleException e
		}
	}

	List<String> getAttributeNames(String sessionId) throws InvalidatedSessionException {
		try {
			PersistentSessionAttribute.findAllNames sessionId
		}
		catch (e) {
			handleException e
		}
	}

	void invalidate(String sessionId) {
		try {
			PersistentSessionAttributeValue.deleteBySessionId sessionId
			PersistentSessionAttribute.deleteBySessionId sessionId

			PersistentSession session = PersistentSession.lock(sessionId)

			def conf = grailsApplication.config.grails.plugin.databasesession
			def deleteInvalidSessions = conf.deleteInvalidSessions ?: false
			if (deleteInvalidSessions) {
				session?.delete()
			}
			else {
				session?.invalidated = true
			}
		}
		catch (e) {
			handleException e
		}
	}

	long getLastAccessedTime(String sessionId) throws InvalidatedSessionException {
		PersistentSession ps = PersistentSession.get(sessionId)
		checkInvalidated ps
		ps.lastAccessedTime
	}

	void setMaxInactiveInterval(String sessionId, int interval) throws InvalidatedSessionException {
		PersistentSession ps = PersistentSession.get(sessionId)
		checkInvalidated ps

		ps.maxInactiveInterval = interval
		if (interval == 0) {
			invalidate sessionId
		}
	}

	int getMaxInactiveInterval(String sessionId) throws InvalidatedSessionException {
		PersistentSession ps = PersistentSession.get(sessionId)
		checkInvalidated ps
		ps.maxInactiveInterval
	}

	boolean isValid(String sessionId) {
		PersistentSession session = PersistentSession.get(sessionId)
		session && session.isValid()
	}

	protected void handleException(e) {
		if (e instanceof InvalidatedSessionException || e instanceof ValidationException) {
			throw e
		}
		GrailsUtil.deepSanitize e
		log.error e.message, e
	}

	protected void checkInvalidated(PersistentSession ps) {
		if (!ps || ps.invalidated) {
			throw new InvalidatedSessionException()
		}
	}

	protected void checkInvalidated(String sessionId) {
		Boolean invalidated = PersistentSession.isInvalidated(sessionId)
		if (invalidated == null || invalidated) {
			throw new InvalidatedSessionException()
		}
	}
}
