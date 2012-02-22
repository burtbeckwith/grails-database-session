package grails.plugin.databasesession

class GormPersisterServiceTests extends GroovyTestCase {

	GormPersisterService gormPersisterService
	def sessionFactory

	private String id = 'abc'

	void testCreateNew() {

		assertNull PersistentSession.get(id)

		long count = PersistentSession.count()

		gormPersisterService.create id

		assertEquals count + 1, PersistentSession.count()

		assertNotNull PersistentSession.get(id)
	}

	void testCreateExisting() {

		long count = PersistentSession.count()

		gormPersisterService.create 'abc'

		assertEquals count + 1, PersistentSession.count()

		gormPersisterService.create 'abc'

		assertEquals count + 1, PersistentSession.count()
	}

	void testGetAttributeNullName() {
		gormPersisterService.create id

		assertNull gormPersisterService.getAttribute('abc', null)
	}

	void testGetAttributeNotFound() {
		gormPersisterService.create id

		assertNull gormPersisterService.getAttribute('abc', 'foo')
	}

	void testGetAttributeInvalidated() {
		gormPersisterService.create id

		PersistentSession.get(id).invalidated = true

		flushAndClear()

		shouldFail(InvalidatedSessionException) {
			gormPersisterService.getAttribute id, 'foo'
		}
	}

	void testGetAttributeOk() {
		String name = 'foo'
		def value = 42

		gormPersisterService.create id
		gormPersisterService.setAttribute id, name, value

		assertEquals value, gormPersisterService.getAttribute(id, name)
	}

	void testSetAttributeNullName() {
		gormPersisterService.create id

		shouldFail(IllegalArgumentException) {
			gormPersisterService.setAttribute 'abc', null, 42
		}
	}

	void testSetAttributeNullValue() {
		gormPersisterService.create id
		gormPersisterService.setAttribute id, 'foo', 42

		assertEquals 42, gormPersisterService.getAttribute(id, 'foo')

		gormPersisterService.setAttribute id, 'foo', null

		assertNull gormPersisterService.getAttribute(id, 'foo')
	}

	void testSetAttributeOk() {
		gormPersisterService.create id
		gormPersisterService.setAttribute id, 'foo', 42

		assertEquals 42, gormPersisterService.getAttribute(id, 'foo')
	}

	void testRemoveAttributeNullName() {

		int count = PersistentSessionAttribute.count()

		gormPersisterService.create id
		gormPersisterService.setAttribute id, 'foo', 42

		assertEquals count + 1, PersistentSessionAttribute.count()

		gormPersisterService.removeAttribute id, null

		assertEquals count + 1, PersistentSessionAttribute.count()
	}

	void testRemoveAttributeInvalidated() {

		int count = PersistentSessionAttribute.count()

		gormPersisterService.create id
		gormPersisterService.setAttribute id, 'foo', 42

		PersistentSession.get(id).invalidated = true

		flushAndClear()

		shouldFail(InvalidatedSessionException) {
			gormPersisterService.removeAttribute id, 'foo'
		}
	}

	void testRemoveAttributeOk() {

		int count = PersistentSessionAttribute.count()

		gormPersisterService.create id
		gormPersisterService.setAttribute id, 'foo', 42

		assertEquals count + 1, PersistentSessionAttribute.count()

		gormPersisterService.removeAttribute id, 'foo'

		assertEquals count, PersistentSessionAttribute.count()
	}

	void testGetAttributeNames() {

		gormPersisterService.create id
		gormPersisterService.setAttribute id, 'foo', 42
		gormPersisterService.setAttribute id, 'bar', 'wahoo'
		gormPersisterService.setAttribute id, 'baz', 'other'

		assertEquals(['bar', 'baz', 'foo'], gormPersisterService.getAttributeNames(id).sort())
	}

	void testInvalidate() {
		gormPersisterService.create id
		gormPersisterService.setAttribute id, 'foo', 42
		gormPersisterService.setAttribute id, 'bar', 'wahoo'

		gormPersisterService.invalidate id

		assertTrue PersistentSession.get(id).invalidated
		assertEquals 0, PersistentSessionAttribute.countBySession(PersistentSession.load(id))

		shouldFail(InvalidatedSessionException) {
			gormPersisterService.getLastAccessedTime id
		}
	}

	void testGetLastAccessedTime() {
		gormPersisterService.create id

		long lastAccessed = gormPersisterService.getLastAccessedTime(id)

		sleep 500

		PersistentSession session = PersistentSession.get(id)

		gormPersisterService.getAttribute(id, 'foo')

		assertTrue gormPersisterService.getLastAccessedTime(id) > lastAccessed
	}

	void testMaxInactiveInterval() {
		gormPersisterService.create id

		assertEquals 30, gormPersisterService.getMaxInactiveInterval(id)

		gormPersisterService.setMaxInactiveInterval id, 15

		assertEquals 15, gormPersisterService.getMaxInactiveInterval(id)

		assertFalse PersistentSession.get(id).invalidated
		gormPersisterService.setMaxInactiveInterval id, 0
		assertTrue PersistentSession.get(id).invalidated
	}

	void testIsValid() {

		assertFalse gormPersisterService.isValid(id)

		gormPersisterService.create id

		assertTrue gormPersisterService.isValid(id)

		gormPersisterService.invalidate id

		assertFalse gormPersisterService.isValid(id)
	}

	boolean isValid(String sessionId) {
		PersistentSession session = PersistentSession.get(sessionId)
		session && !session.invalidated
	}


	private void flushAndClear() {
		sessionFactory.currentSession.flush()
		sessionFactory.currentSession.clear()
	}
}
