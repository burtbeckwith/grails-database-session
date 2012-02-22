package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class DatabaseCleanupService {

	def grailsApplication

	/**
	 * Delete PersistentSessions and corresponding PersistentSessionAttributes where
	 * the last accessed time is older than a cutoff value.
	 */
	void cleanup() {

		def conf = grailsApplication.config.grails.plugin.databasesession
		float maxAge = (conf.cleanup.maxAge ?: 30) as Float

		long age = System.currentTimeMillis() - maxAge * 1000 * 60

		def ids = PersistentSession.findAllByLastAccessedOlderThan(age)
		if (!ids) {
			return
		}

		if (log.isDebugEnabled()) {
			log.debug "using max age $maxAge minute(s), found old sessions to remove: $ids"
		}

		PersistentSessionAttributeValue.deleteBySessionIds ids

		PersistentSessionAttribute.deleteBySessionIds ids

		PersistentSession.deleteByIds ids
	}
}
