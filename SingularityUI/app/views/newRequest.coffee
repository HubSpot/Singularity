View = require './view'

Request = require '../models/Request'

class NewRequest extends View

    template: require '../templates/newRequest'

    events:
        'change input':             'checkForm'
        'keyup input[type="text"]': 'checkForm'
        'click #type .btn':         'changeType'

        'submit form':              'submit'

        'click #reset-button':      'render'

    initialize: ->
        @checkForm = _.debounce @checkForm, 100

        @checkForm()

    render: ->
        @$el.html @template()

        @$('#help-column').css 'height', "#{ @$('form').height() }px"

    changeType: (event) ->
        event.preventDefault()

        $target = $ event.currentTarget

        $target.parents('.btn-group').find('.btn').removeClass 'active'
        $target.addClass 'active'

        @$('.expandable').addClass 'hide'
        @$("\##{ $target.data 'type' }-expandable").removeClass 'hide'

        @checkForm()

    checkForm: ->
        return if @lockdown

        utils.checkMultiInputs @$ '.owner'

        id   = @$('#id').val()
        type = @$('#type .active').data 'type'

        # Make sure all the visible required fields are filled in
        requiredFieldsOkay = true
        for $field in @$ '.required'
            $field = $ $field
            if $field.is(':visible') and not $field.val()
                requiredFieldsOkay = false

        if type and requiredFieldsOkay
            @$('button[type="submit"]').removeAttr 'disabled'
        else
            @$('button[type="submit"]').attr 'disabled', 'disabled'

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

    alert: (message, success = true) ->
        @$('.alert').remove()
        alertClass = if success then 'alert-success' else 'alert-danger'
        @$('form').append "<p class='alert #{ alertClass }'>#{ message }<p>"

module.exports = NewRequest
