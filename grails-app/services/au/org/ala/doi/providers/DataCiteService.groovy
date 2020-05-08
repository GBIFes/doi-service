package au.org.ala.doi.providers

import au.org.ala.doi.util.ServiceResponse
import org.gbif.api.model.common.DOI
import org.gbif.datacite.rest.client.configuration.ClientConfiguration
import grails.util.Holders

import org.gbif.datacite.rest.client.retrofit.DataCiteRetrofitSyncClient
import org.gbif.doi.metadata.datacite.ContributorType
import org.gbif.doi.metadata.datacite.DataCiteMetadata
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles
import org.gbif.doi.metadata.datacite.DateType
import org.gbif.doi.metadata.datacite.DescriptionType
import org.gbif.doi.metadata.datacite.TitleType
import org.gbif.doi.service.datacite.RestJsonApiDataCiteService;

import static org.gbif.doi.metadata.datacite.DataCiteMetadata.*
import static org.gbif.doi.metadata.datacite.DataCiteMetadata.RightsList.*
import static org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.*

class DataCiteService extends DoiProviderService {

    RestJsonApiDataCiteService restDataCiteService
    ClientConfiguration clientConf = ClientConfiguration.builder().withBaseApiUrl(
            Holders.config.datacite.doi.service.baseApiUrl as String).withTimeOut(
            Holders.config.datacite.doi.service.timeOut as long).withFileCacheMaxSizeMb(
            Holders.config.datacite.doi.service.fileCacheMaxSizeMb as long).withUser(
            Holders.config.datacite.doi.service.user as String).withPassword(
            Holders.config.datacite.doi.service.password as String).build()

    DataCiteService() {
        def dataCiteClient = new DataCiteRetrofitSyncClient(clientConf)
        restDataCiteService = new RestJsonApiDataCiteService(dataCiteClient)
        log.info "Using $Holders.config.datacite.doi.service.baseApiUrl baseApiUrl"
    }

    def generateRequestPayload(String uuid, Map metadata, String landingPageUrl, String doi = null) {
        def dcMetadata = new DataCiteMetadata();

        // doi is a mandatory element in the schema, in a ANDS mint request the value is ignored
        // so here we generate one using prefix/shoulder.uuid
        def doiValue = doi ?: "${Holders.config.datacite.doi.service.prefix}/${Holders.config.datacite.doi.service.shoulder}.${uuid}"
        def id = new Identifier()
        id.setValue(doiValue as String)
        id.setIdentifierType("DOI")
        dcMetadata.setIdentifier(id)

        // Creators
        def creators = new Creators()
        if (metadata.creators) {
            for (def creator in metadata.creators) {
                def dcCreator = new Creator()
                def dcCreatorName = new Creator.CreatorName()
                dcCreatorName.setValue(creator.name as String)
                dcCreator.setCreatorName(dcCreatorName)
                creators.creator.add(dcCreator)
            }
        }
        // We add also authors as creators like AndsService
        if (metadata.authors) {
            for (def author in metadata.authors) {
                def dcCreator = new Creator()
                def dcCreatorName = new Creator.CreatorName()
                dcCreatorName.setValue(author as String)
                dcCreator.setCreatorName(dcCreatorName)
                creators.creator.add(dcCreator)
            }
        }
        dcMetadata.setCreators(creators)

        // Titles
        def titles = new Titles()
        if (metadata.titles) {
            for (def title in metadata.titles) {
                def dcTitle = new Title()
                dcTitle.setValue(title.title as String)
                titles.title.add(dcTitle)
            }
        }
        // Adding v3 title as AndsService
        if (metadata.title) {
            def dcTitle = new Title()
            dcTitle.setValue(metadata.title as String)
            titles.title.add(dcTitle)
        }
        if (metadata.subtitle) {
            def sub = new Title();
            sub.setValue("${metadata.subtitle}")
            sub.setTitleType(TitleType.SUBTITLE)
            titles.title.add(sub)
        }
        dcMetadata.setTitles(titles)

        def publisher = new Publisher()
        publisher.setValue("${metadata.publisher}")
        dcMetadata.setPublisher(publisher)

        dcMetadata.setPublicationYear((String) metadata.publicationYear ?: Calendar.getInstance().get(Calendar.YEAR).toString())

        // Subjects
        if (metadata.subjects) {
            def subjects = new Subjects()
            metadata.subjects.each {
                    def subject = new Subjects.Subject()
                    subject.setValue((String) it)
                    subjects.subject.add(subject)
            }
            dcMetadata.setSubjects(subjects)
        }

        // Contributors
        if (metadata.contributors) {
            def contributors = new Contributors()
            for (contrib in metadata.contributors) {
                def contributor = new Contributors.Contributor()
                def contributorType = ContributorType.fromValue(contrib.type as String)
                contributor.setContributorType(contributorType)
                def cName = new Contributors.Contributor.ContributorName()
                cName.setValue(contrib.name as String)
                contributor.setContributorName(cName)
                contributors.contributor.add(contributor)
            }
            dcMetadata.setContributors(contributors)
        }


        // Created dates
        if (metadata.createdDate) {
            def dates = new Dates()
            def createdDate = new Dates.Date()
            createdDate.setDateType(DateType.CREATED)
            createdDate.setValue(metadata.createdDate as String)
            dates.date.add(createdDate)
            dcMetadata.setDates(dates)
        }

        // Rights
        if (metadata.rights) {
            def rightsList = new RightsList()
            metadata.rights.each {
                def rights = new Rights()
                rights.setValue((String) it)
                rightsList.rights.add(rights)
            }
            dcMetadata.setRightsList(rightsList)
        }

        // FIXME put lang in config
        dcMetadata.setLanguage("en")

        // ResourceType
        def resourceType = new ResourceType();
        def resourceTypeGeneral = org.gbif.doi.metadata.datacite.ResourceType.fromValue(metadata.resourceType as String);
        resourceType.setValue(metadata.resourceText as String)
        resourceType.setResourceTypeGeneral(resourceTypeGeneral)
        dcMetadata.setResourceType(resourceType)

        // Descriptions
        if (metadata.descriptions) {
            def descriptions = new Descriptions()
            metadata.descriptions.each {
                def description = new Descriptions.Description()
                def descriptionType = DescriptionType.fromValue(it.type as String)
                description.getContent().add(it.text as String)
                description.setDescriptionType(descriptionType)
                descriptions.description.add(description)
            }
            dcMetadata.setDescriptions(descriptions)
        }

        return dcMetadata
    }

    @Override
    ServiceResponse invokeCreateService(Object requestPayload, String landingPageUrl) {
        def dcMetadata = requestPayload as DataCiteMetadata
        def doiS = dcMetadata.getIdentifier().getValue()
        def doi = new DOI(doiS)
        // restDataCiteService.reserve(doi, dcMetadata)
        restDataCiteService.register(doi, new URI(landingPageUrl), dcMetadata)
        def response = new ServiceResponse(200, '', "")
        response.doi = doiS
        return response
    }

    @Override
    ServiceResponse invokeUpdateService(String doi, Map requestPayload, String landingPageUrl) {
        return successResponse()
    }

    @Override
    ServiceResponse invokeDeactivateService(String doi) {
        service.
        return successResponse()
    }

    @Override
    ServiceResponse invokeActivateService(String doi) {
        return successResponse()
    }

    @Deprecated
    ServiceResponse successResponse() {
        def response = new ServiceResponse(200, '', "ABC")
        response.doi = "10.1000/${UUID.randomUUID()}"
        return response
    }
}