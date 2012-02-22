package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class PersistentSessionAttribute {

	PersistentSession session
	String name

	static void deleteBySessionId(String sessionId) {
		executeUpdate(
			'delete from PersistentSessionAttribute a where a.session.id=:sessionId',
			[sessionId: sessionId])
	}

	static void deleteBySessionIds(sessionIds) {
		executeUpdate(
			'delete from PersistentSessionAttribute a where a.session.id in (:sessionIds)',
			[sessionIds: sessionIds])
	}

	static void remove(String sessionId, String name) {
		executeUpdate(
			'delete from PersistentSessionAttribute psa ' +
			'where psa.session.id=:sessionId and psa.name=:name',
			[sessionId: sessionId, name: name])
	}

	static List<String> findAllNames(String sessionId) {
		PersistentSessionAttribute.executeQuery(
			'select psa.name from PersistentSessionAttribute psa ' +
			'where psa.session.id=:sessionId',
			[sessionId: sessionId])
	}
}
