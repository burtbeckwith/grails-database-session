grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch
grails.project.work.dir = "target/work"
grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
	}

	plugins {
        build(":tomcat:7.0.50", ":release:3.0.1", ":rest-client-builder:1.0.3") {
            export = false
        }
	}
}
