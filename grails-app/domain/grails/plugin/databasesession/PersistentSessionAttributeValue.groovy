package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class PersistentSessionAttributeValue {

	PersistentSessionAttribute attribute
	byte[] serialized

	static constraints = {
	}
}
