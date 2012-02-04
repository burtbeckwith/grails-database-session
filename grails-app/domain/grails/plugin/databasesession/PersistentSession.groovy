package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class PersistentSession {

	String id
	Long creationTime
	Long lastAccessedTime

	static mapping = {
		id generator: 'assigned'
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
}
