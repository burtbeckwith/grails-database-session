package grails.plugin.databasesession

import javax.annotation.PostConstruct;

import grails.util.GrailsUtil
import grails.validation.ValidationException

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.util.Assert

/**
 * @author Burt Beckwith
 */
class GormPersisterService implements Persister {
    
    static transactional = false

    def db
	def grailsApplication
    def mongo
	def persistentSessionService

	void create(String sessionId) {
        println "Create session with $sessionId"
		try {
			if (PersistentSession.exists(sessionId)) {
				return
			}
            Long creationTime = System.currentTimeMillis()

            db.persistentSession.insert([_id: sessionId, creationTime: creationTime,
                lastAccessedTime: creationTime, maxInactiveInterval: 30, invalidated: false])
		} catch (e) {
			handleException e
		}
	}

	Object getAttribute(String sessionId, String name) throws InvalidatedSessionException {
	    println "GET attribure [$name] from [$sessionId]"
		if (name == null) return null

		if (GrailsApplicationAttributes.FLASH_SCOPE == name) {
			// special case; use request scope since a new deserialized instance is created each time it's retrieved from the session
			def fs = SessionProxyFilter.request.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE)
			if (fs != null) {
				return fs
			}
		}

		try {
			PersistentSession session = PersistentSession.get(sessionId)
			checkInvalidated session
			session.lastAccessedTime = System.currentTimeMillis()

			def attribute = persistentSessionService.deserializeAttributeValue(
				persistentSessionService.findValueBySessionAndAttributeName(session, name)?.serialized)
			println "GOT attribute [$name] with $attribute"

			if (attribute != null && GrailsApplicationAttributes.FLASH_SCOPE == name) {
				SessionProxyFilter.request.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, attribute)
			}

			return attribute
		}
		catch (e) {
			handleException e
		}
	}
    
    @PostConstruct
    void postConstruct() {
        println "Initializing GormPersisterService."
        String databaseName = grailsApplication.config.grails.mongo.databaseName

        db = mongo.getDB(databaseName)
    }

	void setAttribute(String sessionId, String name, value) throws InvalidatedSessionException {

		Assert.notNull name, 'name parameter cannot be null'
        println "SET attribute [$name] in [$sessionId] with $value"
        println value?.dump()

		if (value == null) {
			removeAttribute sessionId, name
			return
		}

		// special case; use request scope and don't store in session, the filter will set it in the session at the end of the request
		if (value != null && GrailsApplicationAttributes.FLASH_SCOPE == name) {
			if (value != GrailsApplicationAttributes.FLASH_SCOPE) {
                println ">>> special"
				SessionProxyFilter.request.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, value)
				return
			}

			// the filter set the value as the key, so retrieve it from the request
			value = SessionProxyFilter.request.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE)
		}

		try {
			PersistentSession session = PersistentSession.get(sessionId)
			checkInvalidated session
			session.lastAccessedTime = System.currentTimeMillis()

            PersistentSessionAttribute sessionAttributeInstance =
                    PersistentSessionAttribute.findOrCreateBySessionIdAndName(sessionId, name)

			sessionAttributeInstance.serialized = persistentSessionService.serializeAttributeValue(value)
			sessionAttributeInstance.save(failOnError: true, flush: true)
            println "SETTING attr [$name] $sessionAttributeInstance.id"
		} catch (e) {
			handleException e
		}
	}

	void removeAttribute(String sessionId, String name) throws InvalidatedSessionException {
		if (name == null) return

		try {
			PersistentSession session = PersistentSession.get(sessionId)
			checkInvalidated session
			session.lastAccessedTime = System.currentTimeMillis()

			persistentSessionService.removeAttribute sessionId, name
		}
		catch (e) {
			handleException e
		}
	}

	List<String> getAttributeNames(String sessionId) throws InvalidatedSessionException {
		try {
			persistentSessionService.findAllAttributeNames sessionId
		}
		catch (e) {
			handleException e
		}
	}

	void invalidate(String sessionId) {
		try {
			persistentSessionService.deleteValuesBySessionId sessionId
			persistentSessionService.deleteAttributesBySessionId sessionId

            // TODO Find alternative to lock
			//PersistentSession session = PersistentSession.lock(sessionId)
			PersistentSession session = PersistentSession.get(sessionId)

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
		Boolean invalidated = persistentSessionService.isSessionInvalidated(sessionId)
		if (invalidated == null || invalidated) {
			throw new InvalidatedSessionException()
		}
	}
}