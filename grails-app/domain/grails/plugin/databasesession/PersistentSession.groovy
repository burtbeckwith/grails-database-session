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

	static transients = ['valid']

	static mapping = {
		id generator: 'assigned'
		version false // be sure to lock when changing invalidated but ok to have concurrent updates of lastAccessedTime
		dynamicUpdate true
	}

	boolean isValid() {
		!invalidated && lastAccessedTime > System.currentTimeMillis() - maxInactiveInterval * 1000 * 60
	}
}
