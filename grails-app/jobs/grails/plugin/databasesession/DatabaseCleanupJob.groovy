package grails.plugin.databasesession

import grails.util.Holders

/**
 * @author Burt Beckwith
 */
class DatabaseCleanupJob {

	def databaseCleanupService

	long timeout = 10 * 60 * 1000 // every 10 minutes

	void execute() {
		def enabled = Holders.getFlatConfig()["grails.plugin.databasesession.cleanup.enabled"]
		if (enabled != true) {
			return
		}

		databaseCleanupService.cleanup()
	}
}
