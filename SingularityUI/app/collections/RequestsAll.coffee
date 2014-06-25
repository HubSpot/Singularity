RequestsActive = require "./RequestsActive"

class RequestsAll extends RequestsActive
    
    url: "#{ config.apiRoot }/requests"
                
      
module.exports = RequestsAll