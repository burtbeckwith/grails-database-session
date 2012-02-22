package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class PersistentSession {

	String id
	Long creationTime
	Long lastAccessedTime
	Boolean invalidated = false
	Integer maxInactiveInterval = 30

	static mapping = {
		id generator: 'assigned'
		version false // be sure to lock when changing invalidated but ok to have concurrent updates of lastAccessedTime
		dynamicUpdate true
	}

	boolean isValid() {
		!invalidated && lastAccessedTime > System.currentTimeMillis() - maxInactiveInterval * 1000 * 60
	}

	static List<String> findAllByLastAccessedOlderThan(long age) {
		executeQuery(
			'select s.id from PersistentSession s where s.lastAccessedTime < :age',
			[age: age])
	}

	static void deleteByIds(ids) {
		executeUpdate(
			'delete from PersistentSession s where s.id in (:ids)',
			[ids: ids])
	}

	static Boolean isInvalidated(String sessionId) {
		executeQuery(
			'select s.invalidated from PersistentSession s where s.id=:id',
			[id: sessionId])[0]
	}
}
