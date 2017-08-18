package grails.plugin.databasesession

import org.grails.web.util.GrailsApplicationAttributes

class SessionInterceptor {

    def gormPersisterService

    boolean before() { true }

    boolean after() { true }

    void afterView() {
        if (request.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE) == null) {
            return
        }

        try {
            // set the value to the key as a flag to retrieve it from the request
            gormPersisterService.setAttribute(request.session.id, GrailsApplicationAttributes.FLASH_SCOPE, GrailsApplicationAttributes.FLASH_SCOPE)
        } catch (InvalidatedSessionException ise) {
            // ignored
        }
    }
}
