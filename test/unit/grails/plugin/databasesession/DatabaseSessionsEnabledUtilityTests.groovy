package grails.plugin.databasesession

class DatabaseSessionsEnabledUtilityTests extends GroovyTestCase {

    void "test enabled when there is no configuration"() {
        def config = new ConfigObject()
        assert !DatabaseSessionsEnabledUtility.enabled(config, true)
        assert DatabaseSessionsEnabledUtility.enabled(config, false)
    }

    void "test enabled when there is a configuration"() {
        def config = new ConfigObject()
        config.grails.plugin.databasesession.enabled = true
        assert DatabaseSessionsEnabledUtility.enabled(config, true)
        assert DatabaseSessionsEnabledUtility.enabled(config, false)
    }
}
