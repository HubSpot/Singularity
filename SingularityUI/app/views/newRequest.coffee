FormBaseView = require './formBaseView'

Request = require '../models/Request'

class NewRequest extends FormBaseView

    template: require '../templates/newRequest'

    events: ->
        _.extend super,
            'click #type .btn':         'changeType'

    changeType: (event) ->
        event.preventDefault()

        $target = $ event.currentTarget

        $target.parents('.btn-group').find('.btn').removeClass 'active'
        $target.addClass 'active'

        @$('.expandable').addClass 'hide'
        @$("\##{ $target.data 'type' }-expandable").removeClass 'hide'

        @checkForm()

    submit: (event) ->
        event.preventDefault()
        return if @$('button[type="submit"]').attr 'disabled'
        @$('.alert').remove()

        requestObject = {}

        requestObject.id = @$('#id').val()
        type             = @$('#type .active').data 'type'

        requestObject.owners = @multiList '.owner'

        if type in ['service', 'worker']
            requestObject.daemon = true

            instances                   = parseInt @$("#instances-#{ type }").val()
            requestObject.instances     = instances if instances
            requestObject.rackSensitive = @$("#rack-sensitive-#{ type }").is ':checked'

            if type is 'service'
                requestObject.loadBalanced  = @$('#load-balanced').is ':checked'
        else if type is 'scheduled'
            schedule = @$('#schedule').val()
            retries  = parseInt @$('#retries-on-failure').val()

            if schedule
                requestObject.schedule = schedule
            else
                @alertRequiredField 'schedule'

            requestObject.numRetriesOnFailure = retries if retries
        else if type is 'on-demand'
            requestObject.daemon = false

        return if @invalid

        request = new Request requestObject
        request.url = "#{ config.apiRoot }/requests?user=#{ app.getUsername() }"
        # Fucking Backbone
        request.isNew = -> true

        @lockdown = true
        @$('button[type="submit"]').attr 'disabled', 'disabled'

        serverRequest = request.save()
        serverRequest.done  (response) =>
            @lockdown = false
            @alert "Your Request <a href='#{ config.appRoot }/request/#{ response.id }'>#{ response.id }</a> has been created"

        serverRequest.error (response) =>
            @postSave()

            app.caughtError()
            @alert "There was a problem saving your request. The server response has been logged to your JS console.", false
            console.error response

module.exports = NewRequest
