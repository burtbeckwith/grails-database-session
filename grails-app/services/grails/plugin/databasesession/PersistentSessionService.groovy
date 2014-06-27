package grails.plugin.databasesession

import javax.annotation.PostConstruct;

import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

/**
 * @author Burt Beckwith
 */
class PersistentSessionService {
    
    static transactional = false
    def grailsApplication
    def db
    def mongo

	def deserializeAttributeValue(byte[] serialized) {
		if (!serialized) {
			return null
		}

		// might throw IOException - let the caller handle it
		new ObjectInputStream(new ByteArrayInputStream(serialized)) {
			@Override
			protected Class<?> resolveClass(ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException {
				Class.forName objectStreamClass.name, true, Thread.currentThread().contextClassLoader
			}
		}.readObject()
	}

	byte[] serializeAttributeValue(value) {
		if (value == null) {
			return null
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		// might throw IOException - let the caller handle it
		new ObjectOutputStream(baos).writeObject value
		baos.toByteArray()
	}

	PersistentSessionAttribute findValueBySessionAndAttributeName(PersistentSession session, String name) {
		findValueBySessionIdAndAttributeName(session.id, name)
	}

	PersistentSessionAttribute findValueBySessionIdAndAttributeName(String sessionId, String name) {
        return PersistentSessionAttribute.findBySessionIdAndName(sessionId, name)
	}

	List<PersistentSessionAttribute> findValuesBySession(String sessionId) {
		Assert.hasLength sessionId
        PersistentSessionAttribute.findAllBySessionId(sessionId, [sort: "id"])
	}

	void deleteValuesBySessionId(String sessionId) {
		Assert.hasLength sessionId

		findValuesBySession(sessionId)*.delete()
	}

    @Deprecated
	void deleteValuesBySessionIds(sessionIds) {
		deleteAttributesBySessionIds(sessionIds)
	}

    @Deprecated
	void removeValue(String sessionId, String name) {
		Assert.hasLength sessionId
		Assert.hasLength name
        PersistentSessionAttribute.findAllBySessionIdAndName(sessionId, name)*.delete()
	}

	protected void deleteValuesByIds(ids) {
		if (!ids) {
			return
		}
		PersistentSessionAttribute.getAll(ids)*.delete()
	}

	void deleteAttributesBySessionId(String sessionId) {
        PersistentSessionAttribute.findAllBySessionId(sessionId)*.delete()
	}

	void deleteAttributesBySessionIds(sessionIds) {
        Assert.notEmpty sessionIds

        PersistentSessionAttribute.findAllBySessionIdInList(sessionIds)*.delete()
	}

	void removeAttribute(String sessionId, String name) {
        PersistentSessionAttribute.findBySessionIdAndName(sessionId, name)?.delete(flush: true)
	}

	List<String> findAllAttributeNames(String sessionId) {
        PersistentSessionAttribute.findAllBySessionId(sessionId)*.name
	}

	List<String> findAllSessionIdsByLastAccessedOlderThan(long age) {
        PersistentSession.withCriteria {
            projections {
                property("id")
            }
            lt("lastAccessedTime", age)
        }
	}

	void deleteSessionsByIds(ids) {
        PersistentSession.getAll(ids)*.delete()
	}

	Boolean isSessionInvalidated(String sessionId) {
        PersistentSession.withCriteria {
            projections {
                property("invalidated")
            }
            eq("id", sessionId)
        }
	}

    @PostConstruct
    void postConstruct() {
        String databaseName = grailsApplication.config.grails.mongo.databaseName

        db = mongo.getDB(databaseName)
    }
}