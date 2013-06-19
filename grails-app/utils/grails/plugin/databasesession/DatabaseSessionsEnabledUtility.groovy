package grails.plugin.databasesession

class DatabaseSessionsEnabledUtility {

    static boolean enabled(ConfigObject config, boolean inDevelopment) {
        def enabled = config.grails.plugin.databasesession.enabled
        if (enabled instanceof Boolean) {
            return enabled
        }

        return !inDevelopment
    }
}
