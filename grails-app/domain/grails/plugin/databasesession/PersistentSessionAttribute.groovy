package grails.plugin.databasesession

/**
 * @author Burt Beckwith
 */
class PersistentSessionAttribute {

    String name
    byte[] serialized
    String sessionId

    static constraints = {
        serialized maxSize: 20000
    }
}