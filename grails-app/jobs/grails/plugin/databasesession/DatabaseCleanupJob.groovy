package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class DatabaseCleanupJob {

	def databaseCleanupService
	def grailsApplication

	long timeout = 10 * 60 * 1000 // every 10 minutes

	void execute() {

		def conf = grailsApplication.config.grails.plugin.databasesession
		if (conf.cleanup.enabled instanceof Boolean && !conf.cleanup.enabled) {
			return
		}

		databaseCleanupService.cleanup()
	}
}
