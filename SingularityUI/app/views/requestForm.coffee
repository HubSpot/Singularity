# Handles both creating new tasks
# and updating existing tasks

FormBaseView = require './formBaseView'

Request = require '../models/Request'

class RequestForm extends FormBaseView

    template: require '../templates/requestForm'

    events: ->
        _.extend super,
            'click #type .btn': 'handleChangeType'

    initialize: ({@type, @requestId, @collection}) ->
        super
        if @type is 'edit'
            app.$page.hide()
            @listenTo @collection, 'sync', @renderEditForm

    handleChangeType: (event) ->
        return false if @type is 'edit'
        type = $(event.currentTarget).data 'type'
        @changeType type

    changeType: (type) ->
        @$('#type .btn').removeClass 'active'
        @$("#type [data-type='#{type}']").addClass 'active'
        @$('.expandable').addClass 'hide'
        @$("##{ type }-expandable").removeClass 'hide'

        @checkForm()

    renderEditForm: ->
        @model = @collection?.get @requestId
        request = @model.toJSON()
    
        @render()
        @changeType request.type

        ownerField = (owner) ->
            "<input type='text' value='#{owner}' class='owner form-control' placeholder='eg: user@example.com'>"

        if request.request.owners isnt undefined
            @$('#owner-0').val request.request.owners[0]
            for owner, i in request.request.owners
                continue if i is 0
                @$('#owners').append ownerField(owner)

        if request.type is 'SERVICE' or 'WORKER'
            @$("#instances-#{request.type}").val request.instances
            @$("#rack-sensitive-#{request.type}").prop 'checked', request.request.rackSensitive
            @$("#load-balanced").prop 'checked', request.request.loadBalanced

        if request.type is 'SCHEDULED'
            @$("#schedule").val request.request.schedule

        typeButtons = @$('#type .btn').prop('disabled', true)
        @$("[data-type='#{request.type}']").prop('disabled', false)
        app.$page.show()


    submit: (event) ->
        event.preventDefault()

        return if @$('button[type="submit"]').attr 'disabled'
        @$('.alert').remove()

        requestObject = {}

        requestObject.id = @model?.id or @$('#id').val()

        requestObject.requestType = @$('#type .active').data 'type'
        type = requestObject.requestType
        requestObject.owners = @multiList '.owner'

        if type in ['SERVICE', 'WORKER']
            requestObject.daemon = true

            instances                   = parseInt @$("#instances-#{ type }").val()
            requestObject.instances     = instances if instances
            requestObject.rackSensitive = @$("#rack-sensitive-#{ type }").is ':checked'

            if type is 'SERVICE'
                requestObject.loadBalanced  = @$('#load-balanced').is ':checked'
        else if type is 'SCHEDULED'
            schedule = @$('#schedule').val()
            retries  = parseInt @$('#retries-on-failure').val()

            if schedule
                requestObject.schedule = schedule

            requestObject.numRetriesOnFailure = retries if retries
        else if type is 'ON_DEMAND' or type is 'RUN_ONCE'
            requestObject.daemon = false

        return if @invalid

        request = new Request requestObject
        request.url = "#{ config.apiRoot }/requests?user=#{ app.getUsername() }"
        
        request.isNew = -> true

        @lockdown = true
        @$('button[type="submit"]').attr 'disabled', 'disabled'

        if @model
            @update requestObject
        else
            @create request


    create: (request) ->
        serverRequest = request.save()

        serverRequest.done  (response) =>
            @lockdown = false
            @alert "Your Request <a href='#{ config.appRoot }/request/#{ response.id }'>#{ response.id }</a> has been created"

        serverRequest.error (response) =>
            @postSave()

            app.caughtError()
            @alert "There was a problem saving your request. The server response has been logged to your JS console.", false
            console.error response


    update: (requestObject) ->
        request = @model.toJSON()

        if _.contains ['RUN_ONCE', 'ON_DEMAND'], request.type
            @model.unset 'instances'
        
        @model.url = "#{ config.apiRoot }/requests?user=#{ app.getUsername() }"
        @model.isNew = -> true

        serverRequest = @model.save requestObject

        serverRequest.done  (response) =>
            @lockdown = false
            @alert "Your Request <a href='#{ config.appRoot }/request/#{ response.id }'>#{ response.id }</a> has been updated"

        serverRequest.error (response) =>
            @postSave()

            app.caughtError()
            @alert "There was a problem saving your request. The server response has been logged to your JS console.", false
            console.error response


module.exports = RequestForm
