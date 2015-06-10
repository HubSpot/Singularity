RequestFormBaseView = require './requestFormBase'

class RequestFormNew extends RequestFormBaseView

    handleChangeType: (event) ->
        @requestType = $(event.currentTarget).data 'type'
        @changeType()
        @renderFormElements()

    saveRequest: ->
        serverRequest = @request.save()

        serverRequest.done  (response) =>
            @lockdown = false
            @alert "Your Request <a href='#{ config.appRoot }/request/#{ response.id }'>#{ response.id }</a> has been created"

        serverRequest.error (response) =>
            @postSave()

            app.caughtError()
            @alert "There was a problem saving your request. The server response has been logged to your JS console.", false
            console.error response


module.exports = RequestFormNew