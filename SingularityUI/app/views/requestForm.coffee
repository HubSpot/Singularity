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

        ownersList = _.map request.request.owners, (name, i) -> id: name, text: name
        @ownersInput.select2 'data', ownersList

        if request.type is 'SERVICE' or 'WORKER'
            @$("#instances-#{request.type}").val request.instances
            @$("#rack-sensitive-#{request.type}").prop 'checked', request.request.rackSensitive
            @$("#load-balanced").prop 'checked', request.request.loadBalanced

        if request.type is 'SCHEDULED'
            @$("#schedule").val request.request.schedule

        typeButtons = @$('#type .btn').prop('disabled', true)
        @$("[data-type='#{request.type}']").prop('disabled', false)
        @setEditingRequestTooltips()
        app.$page.show()

    afterRender: ->
        @renderFormElements()
        @setTooltips()

    renderFormElements: ->
        @ownersInput = @$("#owners").select2
            tags: true
            tokenSeparators: [',',' ','\n','\t']

        @$('#rackAffinity-SERVICE').select2
            tags: true
            tokenSeparators: [',',' ','\n','\t']

        @$('#rackAffinity-WORKER').select2
            tags: true
            tokenSeparators: [',',' ','\n','\t']

    setTooltips: ->
        @$(".tagging-input").tooltip
            title: 'Comma separate values'
            placement: 'top'

    setEditingRequestTooltips: ->
        @$("[data-tooltip='rack-sensitive']").tooltip
            title: 'Changes will only affect new tasks.'
            placement: 'top'

    submit: (event) ->
        event.preventDefault()

        return if @$('button[type="submit"]').attr 'disabled'
        @$('.alert').remove()

        requestObject = {}

        requestObject.id = @model?.id or @$('#id').val()

        requestObject.requestType = @$('#type .active').data 'type'
        type = requestObject.requestType

        requestObject.owners = @taggableList '#owners'

        slavePlacement = @$('#slavePlacement').val()
        if slavePlacement.length > 0
            requestObject.slavePlacement = slavePlacement

        if type in ['SERVICE', 'WORKER']
            requestObject.daemon = true

            instances                   = parseInt @$("#instances-#{ type }").val()
            requestObject.instances     = instances if instances

            waitAtLeastMillisAfterTaskFinishesForReschedule = parseInt @$("#waitAtLeastMillisAfterTaskFinishesForReschedule-#{ type }").val()
            requestObject.waitAtLeastMillisAfterTaskFinishesForReschedule = waitAtLeastMillisAfterTaskFinishesForReschedule if waitAtLeastMillisAfterTaskFinishesForReschedule

            requestObject.rackSensitive = @$("#rack-sensitive-#{ type }").is ':checked'
            
            requestObject.rackAffinity = @taggableList "#rackAffinity-#{ type }"

        if type in ['SCHEDULED', 'ON_DEMAND', 'RUN_ONCE']
            requestObject.killOldNonLongRunningTasksAfterMillis = parseInt @$("#killOldNRL-#{ type }").val()

        if type in ['ON_DEMAND', 'RUN_ONCE']
            requestObject.daemon = false

        if type is 'SCHEDULED'
            retries  = parseInt @$('#retries-on-failure').val()
            schedule = @$('#schedule').val()
            expectedRuntime = parseInt @$('#scheduled-expected-runtime').val()

            requestObject.schedule = schedule if schedule
            requestObject.numRetriesOnFailure = retries if retries
            requestObject.scheduledExpectedRuntimeMillis = expectedRuntime if expectedRuntime

        if type is 'SERVICE'
            requestObject.loadBalanced  = @$('#load-balanced').is ':checked'

        # TO DO: Add validation
        ## killOldNonLongRunningTasksAfterMillis 
        ## scheduledExpectedRuntimeMillis
        ## waitAtLeastMillisAfterTaskFinishesForReschedule

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
