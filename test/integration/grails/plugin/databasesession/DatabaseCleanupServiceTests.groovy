package grails.plugin.databasesession

class DatabaseCleanupServiceTests extends GroovyTestCase {

	DatabaseCleanupService databaseCleanupService
	def grailsApplication
	def sessionFactory

	protected void setUp() {
		super.setUp()

		def session = new PersistentSession(creationTime: 0, lastAccessedTime: System.currentTimeMillis())
		session.id = 's1'
		session.save()
		def attr = new PersistentSessionAttribute(session: session, name: 'a1').save()
		new PersistentSessionAttributeValue(attribute: attr, value: 'v1').save()

		session = new PersistentSession(creationTime: 0, lastAccessedTime: System.currentTimeMillis())
		session.id = 's2'
		session.save()
		attr = new PersistentSessionAttribute(session: session, name: 'a2').save()
		new PersistentSessionAttributeValue(attribute: attr, value: 'v2').save()

		session = new PersistentSession(creationTime: 0, lastAccessedTime: System.currentTimeMillis())
		session.id = 's3'
		session.save()

		flushAndClear()
	}

	void testCleanup() {

		assertEquals 3, PersistentSession.count()
		assertEquals 2, PersistentSessionAttribute.count()
		assertEquals 2, PersistentSessionAttributeValue.count()

		def session = PersistentSession.get('s1')
		session.lastAccessedTime = System.currentTimeMillis() - 20000
		session.save(flush: true)

		databaseCleanupService.cleanup()

		assertEquals 3, PersistentSession.count()
		assertEquals 2, PersistentSessionAttribute.count()
		assertEquals 2, PersistentSessionAttributeValue.count()

		session.lastAccessedTime = System.currentTimeMillis() - 2000000
		session.save(flush: true)

		databaseCleanupService.cleanup()

		assertEquals 2, PersistentSession.count()
		assertEquals 1, PersistentSessionAttribute.count()
		assertEquals 1, PersistentSessionAttributeValue.count()
	}

	private void flushAndClear() {
		sessionFactory.currentSession.flush()
		sessionFactory.currentSession.clear()
	}
}
