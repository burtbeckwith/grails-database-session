package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class DatabaseCleanupService {

	def grailsApplication
	def persistentSessionService

	/**
	 * Delete PersistentSessions and corresponding PersistentSessionAttributes where
	 * the last accessed time is older than a cutoff value.
	 */
	void cleanup() {

		def conf = grailsApplication.config.grails.plugin.databasesession
		float maxAge = (conf.cleanup.maxAge ?: 30) as Float

		long age = System.currentTimeMillis() - maxAge * 1000 * 60

		def ids = persistentSessionService.findAllSessionIdsByLastAccessedOlderThan(age)
		if (!ids) {
			return
		}

		if (log.isDebugEnabled()) {
			log.debug "using max age $maxAge minute(s), found old sessions to remove: $ids"
		}

		persistentSessionService.deleteValuesBySessionIds ids

		persistentSessionService.deleteAttributesBySessionIds ids

		persistentSessionService.deleteSessionsByIds ids
	}
}
