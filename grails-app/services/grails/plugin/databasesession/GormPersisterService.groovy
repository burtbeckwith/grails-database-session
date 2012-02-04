package grails.plugin.databasesession

import grails.util.GrailsUtil

/**
 * @author Burt Beckwith
 */
class GormPersisterService implements Persister {

	void create(String sessionId, long creationTime) {
		try {
			if (PersistentSession.exists(sessionId)) {
				return
			}

			PersistentSession session = new PersistentSession()
			session.creationTime = creationTime
			session.lastAccessedTime = creationTime
			session.id = sessionId
			session.save()
		}
		catch (e) {
			handleException e
		}
	}

	Object getAttribute(String sessionId, String name, long lastAccessedTime) {
		try {
			PersistentSessionAttribute attr = get(sessionId, name)
			if (!attr) {
				return null
			}

			attr.session.lastAccessedTime = lastAccessedTime
			return attr.value
		}
		catch (e) {
			handleException e
		}
	}

	void setAttribute(String sessionId, String name, value, long lastAccessedTime) {
		try {
			def attr = get(sessionId, name)
			if (!attr) {
				attr = new PersistentSessionAttribute(
					session: PersistentSession.get(sessionId), name: name)
			}
			attr.session.lastAccessedTime = lastAccessedTime
			attr.value = value
			attr.save()
		}
		catch (e) {
			handleException e
		}
	}

	void removeAttribute(String sessionId, String name, long lastAccessedTime) {
		try {
			PersistentSession.get(sessionId).lastAccessedTime = lastAccessedTime

			PersistentSessionAttribute.executeUpdate(
				'delete from PersistentSessionAttribute psa ' +
				'where psa.session.id=:sessionId and psa.name=:name',
				[sessionId: sessionId, name: name])
		}
		catch (e) {
			handleException e
		}
	}

	List<String> getAttributeNames(String sessionId) {
		try {
			PersistentSessionAttribute.executeQuery(
				'select psa.name from PersistentSessionAttribute psa ' +
				'where psa.session.id=:sessionId',
				[sessionId: sessionId])
		}
		catch (e) {
			handleException e
		}
	}

	void invalidate(String sessionId) {
		try {
			PersistentSessionAttribute.executeUpdate(
				'delete from PersistentSessionAttribute psa ' +
				'where psa.session.id=:sessionId',
				[sessionId: sessionId])

			PersistentSession.executeUpdate(
				'delete from PersistentSession ps where ps.id=:id',
				[id: sessionId])
		}
		catch (e) {
			handleException e
		}
	}

	protected PersistentSessionAttribute get(String sessionId, String name) {
		PersistentSessionAttribute.findBySessionAndName(
			PersistentSession.get(sessionId), name)
	}

	protected void handleException(e) {
		GrailsUtil.deepSanitize e
		log.error e.message, e
	}
}
