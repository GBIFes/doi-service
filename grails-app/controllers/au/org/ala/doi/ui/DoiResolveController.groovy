package au.org.ala.doi.ui

import au.org.ala.doi.BasicWSController
import au.org.ala.doi.Doi
import au.org.ala.doi.DoiService
import au.org.ala.doi.storage.Storage
import au.org.ala.web.AuthService
import au.org.ala.web.UserDetails
import au.org.ala.ws.validation.constraints.UUID
import com.google.common.io.ByteSource
import org.springframework.web.context.request.RequestContextHolder
import grails.converters.XML

import javax.validation.constraints.NotNull

class DoiResolveController extends BasicWSController {

    static final int DEFAULT_PAGE_SIZE = 20
    DoiService doiService
    Storage storage
    AuthService authService


    def index() {
        int pageSize = params.getInt("pageSize", DEFAULT_PAGE_SIZE)
        int offset = params.getInt("offset", 0)

        def isAdmin = RequestContextHolder.currentRequestAttributes()?.isUserInRole("ROLE_ADMIN")

        render view: "index", model: [dois    : doiService.listDois(pageSize, offset, "dateMinted", "desc", !isAdmin ? [active:true]:null),
                                      offset  : offset,
                                      pageSize: pageSize,
                                      isAdmin : isAdmin]
    }

    def doi(@NotNull @UUID String id) {
        Doi doi = doiService.findByUuid(id)

        def isAdmin = RequestContextHolder.currentRequestAttributes()?.isUserInRole("ROLE_ADMIN")

        if (doi) {
            render view: "doi", model: [doi: doi, isAdmin : isAdmin]
        } else {
            notFound "No DOI record was found for UUID ${id}"
        }
    }

    def download(@NotNull @UUID String id) {
        Doi doi = doiService.findByUuid(id)

        if (!doi) {
            notFound "No doi was found for ${id}"
        } else {

            // if the DOI is restricted, let's drill down to see if the user trying to access the file should be allowed
            // Requestor in this context is the user trying to download the file
            if(doi.authorisedRoles){
                String requestorId = authService.userId
                if(!requestorId) {
                    // Force authentication for the same download
                    // This method will end up being called again but with an authenticated user.
                    redirect uri: "/doi/${doi.uuid}/downloadAuth"
                    log.debug("File for DOI ${doi.doi} (uuid = ${doi.uuid}) is not public, user needs to login to see if he is permitted to download it.")
                    return
                } else if(requestorId != doi.userId) {
                    UserDetails userDetails = authService.getUserForUserId(requestorId)
                    if(!userDetails.roles.containsAll(doi.authorisedRoles)) {
                        //Redirect to not allowed page.
                        render view: "unauthorisedDownload", model: [doi: doi] //TODO
                        log.debug("File for DOI ${doi.doi} (uuid = ${doi.uuid}) is not public and user ${requestorId} does not have the apropriate permissions to access it")
                        return
                    }
                }
            }

            ByteSource byteSource = storage.getFileForDoi(doi)
            if (byteSource) {
                response.setContentType(doi.contentType)
                response.setHeader("Content-disposition", "attachment;filename=${doi.filename}")
                byteSource.openStream().withStream {
                    response.outputStream << it
                }
                response.outputStream.flush()
            } else {
                notFound "No file was found for DOI ${doi.doi} (uuid = ${doi.uuid})"
            }
        }
    }
}
