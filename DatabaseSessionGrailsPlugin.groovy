import grails.plugin.databasesession.SessionProxyFilter
import grails.util.Metadata

import org.springframework.web.filter.DelegatingFilterProxy

class DatabaseSessionGrailsPlugin {
	String version = '1.0'
	String grailsVersion = '1.3.3 > *'
	String title = 'Database Session Plugin'
	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String description = 'Stores HTTP sessions in a database'
	String documentation = 'http://grails.org/plugin/database-session'

	String license = 'APACHE'
	def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPDATABASESESSION']
	def scm = [url: 'https://github.com/burtbeckwith/grails-database-session']

	// make sure the filter is first
	def getWebXmlFilterOrder() {
		[sessionProxyFilter: -100]
	}

	def doWithWebDescriptor = { xml ->

		// add the filter after the last context-param
		def contextParam = xml.'context-param'

		contextParam[contextParam.size() - 1] + {
			'filter' {
				'filter-name'('sessionProxyFilter')
				'filter-class'(DelegatingFilterProxy.name)
			}
		}

		def filter = xml.'filter'
		filter[filter.size() - 1] + {
			'filter-mapping' {
				'filter-name'('sessionProxyFilter')
				'url-pattern'('/*')
				'dispatcher'('ERROR')
				'dispatcher'('FORWARD')
				'dispatcher'('REQUEST')
			}
		}
	}

	def doWithSpring = {

		// do the check here instead of doWithWebDescriptor to get more obvious error display
		if (Metadata.current.getApplicationName() && !manager.hasGrailsPlugin('webxml')) {
			throw new IllegalStateException('The database-session plugin requires that the webxml plugin be installed')
		}

		sessionProxyFilter(SessionProxyFilter) {
			persister = ref('gormPersisterService')
		}
	}
}
