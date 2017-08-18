package grails.plugin.databasesession

import grails.util.Holders

/**
 * @author Burt Beckwith
 */
class DatabaseCleanupJob {

	DatabaseCleanupService databaseCleanupService

	static triggers = {
		simple repeatInterval: 10 * 60 * 1000l // execute job once in 10 minutes
	}

	def execute() {
		Boolean enabled = Holders.getFlatConfig()["grails.plugin.databasesession.cleanup.enabled"]
		if (!enabled) {
			return
		}

		databaseCleanupService.cleanup()
	}
}
