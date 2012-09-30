grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch
grails.project.source.level = 1.6

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
	}

	plugins {

		runtime ":hibernate:$grailsVersion"

		build(':release:2.0.4', ':rest-client-builder:1.0.2') {
			export = false
		}
	}
}
