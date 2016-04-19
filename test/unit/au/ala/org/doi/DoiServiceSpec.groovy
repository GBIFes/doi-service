package au.ala.org.doi

import au.org.ala.doi.Doi
import au.org.ala.doi.DoiService
import au.org.ala.doi.EmailService
import au.org.ala.doi.FileService
import au.org.ala.doi.providers.AndsService
import au.org.ala.doi.util.DoiProvider
import com.sun.org.apache.xpath.internal.operations.Mult
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

@TestFor(DoiService)
@Mock([Doi])
class DoiServiceSpec extends Specification {
    DoiService service

    def setup() {
        service = new DoiService()
        service.andsService = Mock(AndsService)
        service.emailService = Mock(EmailService)
        service.fileService = Mock(FileService)

        service.grailsApplication = [
                config: [
                        support: [
                                email: "supportEmail"
                        ]
                ]
        ]
    }

    def "mintDoi should throw IllegalArgumentException if no provider is given"() {
        when:
        service.mintDoi(null, [foo: "bar"], "title", "authors", "description", "applicationurl", "url", null)

        then:
        thrown IllegalArgumentException
    }

    def "mintDoi should throw IllegalArgumentException if no providerMetada is given"() {
        when:
        service.mintDoi(DoiProvider.ANDS, [:], "title", "authors", "description", "applicationurl", "url", null)

        then:
        thrown IllegalArgumentException
    }

    def "mintDoi should throw IllegalArgumentException if no applicationUrl is given"() {
        when:
        service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", null, "url", null)

        then:
        thrown IllegalArgumentException
    }

    def "mintDoi should throw IllegalStateException if the local database record cannot be validated"() {
        when: "given data with missing mandatory entity parameters (i.e. no description)"
        service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", null, "applicationUrl", null, Mock(MultipartFile))

        then:
        thrown IllegalStateException
    }

    def "mintDoi should invoke the ANDS provider service when DoiProvider == ANDS and the metadata is valid"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        when:
        service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", "applicationUrl", "url", file)

        then:
        1 * service.andsService.mintDoi(_, _, _) >> "newDoi"
    }

    def "mintDoi should send an alert email to support if the DB save fails after the DOI was generated"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        service.andsService.mintDoi(_, _, _) >> null

        when:
        service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", "applicationUrl", "url", file)

        then:
        1 * service.emailService.sendEmail("supportEmail", "doiservice<no-reply@ala.org.au>", _, _)
    }

    def "mintDoi should return a map with an error message if the DB save fails after the DOI was generated"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        service.andsService.mintDoi(_, _, _) >> null

        when:
        Map result = service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", "applicationUrl", "url", file)

        then:
        result.error != null
        !result.uuid
    }

    def "mintDoi should return a map with the DOI, landingPageUrl and the UUID on success"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        service.andsService.mintDoi(_, _, _) >> "newDoi"
        service.andsService.generateLandingPageUrl(_, _) >> "http://landingpage.com"

        when:
        Map result = service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", "applicationUrl", "url", file)

        then:
        !result.error
        result.uuid != null
        result.doi == "newDoi"
        result.landingPage != null
    }
}