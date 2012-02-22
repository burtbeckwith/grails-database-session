package grails.plugin.databasesession

import org.springframework.util.Assert

/**
 * @author Burt Beckwith
 */
class PersistentSessionAttributeValue {

	PersistentSessionAttribute attribute
	byte[] serialized

	static transients = ['value']

	def getValue() {
		// might throw IOException - let the caller handle it
		serialized ? new ObjectInputStream(new ByteArrayInputStream(serialized)).readObject() : null
	}

	void setValue(value) {
		if (value == null) {
			serialized = null
			return
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		// might throw IOException - let the caller handle it
		new ObjectOutputStream(baos).writeObject value
		serialized = baos.toByteArray()
	}

	static constraints = {
		serialized maxSize: 20000
	}

	static PersistentSessionAttributeValue findBySessionAndAttributeName(
			PersistentSession session, String name) {
		PersistentSessionAttributeValue psav = PersistentSessionAttribute.executeQuery(
				'from PersistentSessionAttributeValue v ' +
				'where v.attribute.session=:session and v.attribute.name=:name',
				[session: session, name: name])[0]
	}

	static void deleteBySessionId(String sessionId) {
		Assert.hasLength sessionId
//		executeUpdate(
//			'delete from PersistentSessionAttributeValue v ' +
//			'where v.attribute.session.id=:sessionId',
//			[sessionId: sessionId])
		deleteByIds(executeQuery(
			'select id from PersistentSessionAttributeValue v ' +
			'where v.attribute.session.id=:sessionId',
			[sessionId: sessionId]))
	}

	static void deleteBySessionIds(sessionIds) {
		Assert.notEmpty sessionIds

//		executeUpdate(
//			'delete from PersistentSessionAttributeValue v ' +
//			'where v.attribute.session.id in (:sessionIds)',
//			[sessionIds: sessionIds])
		deleteByIds(executeQuery(
			'select id from PersistentSessionAttributeValue v ' +
			'where v.attribute.session.id in (:sessionIds)',
			[sessionIds: sessionIds]))
	}

	static void remove(String sessionId, String name) {
		Assert.hasLength sessionId
		Assert.hasLength name

		// TODO generates broken SQL: delete from persistent_session_attribute_value cross join persistent_session_attribute persistent1_ where session_id=?
//		executeUpdate(
//			'delete from PersistentSessionAttributeValue v ' +
//			'where v.attribute.session.id=:sessionId and v.attribute.name=:name',
//			[sessionId: sessionId, name: name])
		deleteByIds(executeQuery(
			'select id from PersistentSessionAttributeValue v ' +
			'where v.attribute.session.id=:sessionId and v.attribute.name=:name',
			[sessionId: sessionId, name: name]))
	}

	private static void deleteByIds(ids) {
		if (!ids) {
			return
		}

		executeUpdate(
			'delete from PersistentSessionAttributeValue v where v.id in (:ids)',
			[ids: ids])
	}
}
