package grails.plugin.databasesession

import grails.util.Environment
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

/**
 * @author Burt Beckwith
 */
class SessionFilters {

	def gormPersisterService

	def filters = {

		flash(controller:'*', action:'*') {

			afterView = { Exception e ->
                boolean enabled = DatabaseSessionsEnabledUtility.enabled(grailsApplication.config,
                        Environment.isDevelopmentMode())

                if(!enabled) {
                    return
                }

                if (request.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE) == null) {
					return
				}

				try {
					// set the value to the key as a flag to retrieve it from the request
					gormPersisterService.setAttribute(request.session.id, GrailsApplicationAttributes.FLASH_SCOPE, GrailsApplicationAttributes.FLASH_SCOPE)
				}
				catch (InvalidatedSessionException ise) {
					// ignored
				}
			}
		}
	}
}
