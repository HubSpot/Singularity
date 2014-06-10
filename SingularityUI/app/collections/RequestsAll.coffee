RequestsActive = require "./RequestsActive"

class RequestsAll extends RequestsActive
    
    url: "#{ config.apiRoot }/requests",
    comparator: "timestamp"
    
    # RequestsActive & its subclasses share a parse(), while RequestsCleaning and RequestsPending have
    #    their own. When fetching all Requests, this parse() uses the appropriate class' parse() on each request
    parse: (requests) =>
        _.each requests, (request, i) =>
            if request.request.cleanupType?
                parsingClass = app.collections.requestsCleaning
            else if request.request.requestDeployState? and request.request.requestDeployState.pendingDeploy?
                parsingClass = app.collections.requestsPending
            else
                parsingClass = app.collections.requestsActive
            
            parsed = parsingClass.parse([request])
            requests[i] = parsed[0]

        requests
      
module.exports = RequestsAll