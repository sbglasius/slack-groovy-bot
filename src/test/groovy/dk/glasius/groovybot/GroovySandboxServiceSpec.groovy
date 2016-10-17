package dk.glasius.groovybot

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(GroovySandboxService)
class GroovySandboxServiceSpec extends Specification {

    def setup() {
        service.setup()
    }

    def cleanup() {
    }

    void "test execution with result"() {
        when:
        def result = service.executeScript('''
            return "Groovy is cool!"
        ''')

        then:
        result.executionResult == 'Groovy is cool!'
        result.outputText == ''
    }

    void "test execution with result and output"() {
        when:
        def result = service.executeScript('''
            println "GR8 Stuff"
            x = "Groovy"
            "$x is cool!"
        ''')

        then:
        result.executionResult == 'Groovy is cool!'
        result.outputText == 'GR8 Stuff\n'
    }

    void "test compile error execution"() {
        when:
        def result = service.executeScript('''bogus.code''')

        then:
        result.stacktraceText.startsWith('groovy.lang.MissingPropertyException: No such property: bogus for class: Script1')
    }

    @Unroll('test that executing "#script" is prevented')
    void "test that executing script is prevented"() {
        when:
        def result = service.executeScript(script)

        then:
        result.stacktraceText.startsWith(error)
        where:
        script                            | error
        'System.exit()'                   | 'java.lang.SecurityException: No access to java.lang.System'
        "System.getProperty('key')"       | 'java.lang.SecurityException: No access to java.lang.System'
        "System.setProperty('key','val')" | 'java.lang.SecurityException: No access to java.lang.System'
        "Runtime.runtime.exec('ls')"      | 'java.lang.SecurityException: No access to property java.lang.Runtime.runtime'
        'Runtime.currentRuntime'          | 'java.lang.SecurityException: No access to property java.lang.Runtime.currentRuntime'
        '"ls".execute()'                  | 'java.lang.SecurityException: Do not call java.lang.String.execute()'
        'Runtime.runFinalization0()'      | 'java.lang.SecurityException: No access to java.lang.Runtime'
        'File.newInstance()'              | 'java.lang.SecurityException: No access to java.io.File'
    }

    @Unroll('test that access with "#script" to certain properties are prevented')
    void 'test that access to certain properties is prevented'() {
        when:
        def result = service.executeScript(script)

        then:
        result.stacktraceText.startsWith(error)

        where:
        script                                            | error
        "String.metaClass.kryf = {}"                      | "java.lang.SecurityException: No access to property java.lang.String.metaClass"
        "File.metaClass.kryf = {}"                        | "java.lang.SecurityException: No access to property java.io.File.metaClass"
        "String.metaClass = new ExpandoMetaClass(String)" | "java.lang.SecurityException: No access to property java.lang.String.metaClass"
    }

    @Unroll("test that class outside '#script' cannot be called")
    void 'test that classes outside given package cannot be called'() {
        when:
        def result = service.executeScript(script)

        then:
        result.stacktraceText.startsWith(error)

        where:
        script                                                 | error
        "grails.util.Environment.current"                      | "java.lang.SecurityException: No access to grails.util.Environment since the class is not in 'java.', 'groovy.' or 'spock.' packages"
        "org.apache.commons.io.IOUtils.toString([] as Byte[])" | "java.lang.SecurityException: No access to org.apache.commons.io.IOUtils since the class is not in 'java.', 'groovy.' or 'spock.' packages"
    }

    @Unroll('prevent access to URL protocol not http(s)')
    void 'prevent access to URL protocol not http(s)'() {
        when:
        def result = service.executeScript(script)

        then:
        result.stacktraceText.startsWith(error)

        where:
        script                                                | error
        '"file:/etc/passwd".toURL().newInputStream()'         | 'java.lang.SecurityException: No access to URL/URI protocols not starting with http'
        '"file:/etc/passwd".toURI().toURL().newInputStream()' | 'java.lang.SecurityException: No access to URL/URI protocols not starting with http'
    }

    def x() {
        org.apache.commons.io.IOUtils.toString(new URL('file:/etc/passwd'))
    }


    private static getScriptWithGreb() {
        '''
            @Grab('org.apache.httpcomponents:httpclient:4.2.1')
            import org.apache.http.impl.client.DefaultHttpClient
            import org.apache.http.client.methods.HttpGet
            
            def httpClient = new DefaultHttpClient()
            def url = 'http://www.google.com/search?q=Groovy\'
            def httpGet = new HttpGet(url)
            
            def httpResponse = httpClient.execute(httpGet)
        '''.stripIndent()
    }

}
