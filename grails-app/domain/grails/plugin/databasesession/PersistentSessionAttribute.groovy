package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class PersistentSessionAttribute {

	PersistentSession session
	String name
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

	static void deleteBySessionIds(sessionIds) {
		executeUpdate(
			'delete from PersistentSessionAttribute a where a.session.id in (:sessionIds)',
			[sessionIds: sessionIds])

	}
}
