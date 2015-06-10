# Handles both creating new tasks
# and updating existing tasks

FormBaseView = require './formBaseView'

Request = require '../models/Request'

class RequestForm extends FormBaseView

    template: require '../templates/requestForm'

    # A list of request inputs that we want taggable
    taggables: ->
        type = @model?.get('request')?.requestType || @requestType
        racks = _.pluck @racks.toJSON(), 'id'
        [
            { name: 'owners', selector: '#owners', tags: true }
            { name: 'rackAffinity', selector: "#rackAffinity-#{type}", tags: racks  }
        ]

    events: ->
        _.extend super,
            'click #type .btn': 'handleChangeType'

    initialize: ({@type, @requestId, @racks}) ->
        super

    handleChangeType: (event) ->
        return false if @type is 'edit'
        @requestType = $(event.currentTarget).data 'type'
        @changeType()
        @renderFormElements()
    
    # Expands a given request type form fields
    changeType: ->
        @$('#type .btn').removeClass 'active'
        @$("#type [data-type='#{ @requestType }']").addClass 'active'
        @$('.expandable').addClass 'hide'
        @$("##{ @requestType }-expandable").removeClass 'hide'
        @checkForm()

    renderEditForm: ->
        request = @model.toJSON()

        @requestType = request.type
        # render our request models info
        @render()
        # expand appropriate form fields
        @changeType()

        # render taggable data
        for tag in @taggables()
            @renderTaggable @[tag.name], request.request[tag.name]
        
        @$('#slavePlacement').val request.request.slavePlacement
        
        if request.type is 'SERVICE' or 'WORKER'
            @$("#instances-#{request.type}").val request.instances
            @$("#rack-sensitive-#{request.type}").prop 'checked', request.request.rackSensitive
            @$("#load-balanced").prop 'checked', request.request.loadBalanced
            @$("#waitAtLeast-#{request.type}").val request.request.waitAtLeastMillisAfterTaskFinishesForReschedule

        if request.type is 'SCHEDULED'
            @$("#schedule").val request.request.schedule
            @$("#retries-on-failure").val request.request.numRetriesOnFailure
            @$("#scheduled-expected-runtime").val request.request.scheduledExpectedRuntimeMillis

        if request.type in ['SCHEDULED','ON_DEMAND','RUN_ONCE']
            @$("#killOldNRL-#{request.type}").val request.request.killOldNonLongRunningTasksAfterMillis

        typeButtons = @$('#type .btn').prop('disabled', true)
        @$("[data-type='#{request.type}']").prop('disabled', false)
        @setEditingRequestTooltips()

    afterRender: ->
        @renderFormElements() if @model or @type isnt 'edit'

    renderFormElements: ->
        for input in @taggables()
            @[input.name] = @$("#{input.selector}").select2
                tags: input.tags
                tokenSeparators: [',',' ','\n','\t']

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

            waitAtLeast = parseInt @$("#waitAtLeast-#{ type }").val()
            requestObject.waitAtLeastMillisAfterTaskFinishesForReschedule = waitAtLeast if waitAtLeast

            requestObject.rackSensitive = @$("#rack-sensitive-#{ type }").is ':checked'
            
            requestObject.rackAffinity = @taggableList "#rackAffinity-#{ type }"

        if type in ['SCHEDULED', 'ON_DEMAND', 'RUN_ONCE']
            killOld = parseInt @$("#killOldNRL-#{ type }").val()
            requestObject.killOldNonLongRunningTasksAfterMillis = killOld if killOld

        if type in ['ON_DEMAND', 'RUN_ONCE']
            requestObject.daemon = false

        if type is 'SCHEDULED'
            schedule = @$('#schedule').val()
            retries  = parseInt @$('#retries-on-failure').val()
            expectedRuntime = parseInt @$('#scheduled-expected-runtime').val()

            requestObject.schedule = schedule if schedule
            requestObject.numRetriesOnFailure = retries if retries
            requestObject.scheduledExpectedRuntimeMillis = expectedRuntime if expectedRuntime

        if type is 'SERVICE'
            requestObject.loadBalanced  = @$('#load-balanced').is ':checked'


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
