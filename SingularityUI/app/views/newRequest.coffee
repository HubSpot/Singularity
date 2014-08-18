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

        requestObject.owners = []
        for $owner in @$ '.owner'
            owner = $($owner).val() 
            requestObject.owners.push owner if owner

        if type is 'service'
            requestObject.daemon = true

            instances                   = parseInt @$('#instances').val()
            requestObject.rackSensitive = @$('#rack-sensitive').is ':checked'
            requestObject.loadBalanced  = @$('#load-balanced').is ':checked'

            requestObject.instances = instances if instances
        else if type is 'worker'
            requestObject.daemon = true
        else if type is 'scheduled'
            schedule = @$('#schedule').val()
            retries  = parseInt @$('#retries-on-failure').val()

            if schedule
                requestObject.schedule = schedule
            else
                @alertRequiredField 'schedule'

            requestObject.numRetriesOnFailure = retries if retries

        requestObject.daemon = false unless requestObject.daemon

        return if @invalid

        request = new Request requestObject
        request.url = "#{ config.apiRoot }/requests?user=#{ app.getUsername() }"
        # Fucking Backbone
        request.isNew = -> true

        @lockdown = true
        @$('button[type="submit"]').addClass 'disabled'

        serverRequest = request.save()
        serverRequest.done  (response) =>
            @lockdown = false
            @checkForm()

            @alert "Your Request <a href='#{ config.appRoot }/request/#{ response.id }'>#{ response.id }</a> has been created"
            @$('#reset-button').removeClass 'hide'
        
        serverRequest.error (response) =>
            @lockdown = false
            @checkForm()

            app.caughtError()
            @alert "There was a problem saving your request. The server response has been logged to your JS console.", false
            @$('#reset-button').removeClass 'hide'
            console.error response

module.exports = NewRequest
