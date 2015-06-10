# Base View for creating and editing requests
# extended by requestFormEdit and requestFormNew

FormBaseView = require './formBaseView'

Request = require '../models/Request'

class RequestFormNew extends FormBaseView

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

    handleChangeType: (event) ->
        event.preventDefault() if event

    initialize: ({@requestId, @racks}) ->
        super

    renderFormElements: ->
        for input in @taggables()
            @[input.name] = @$("#{input.selector}").select2
                tags: input.tags
                tokenSeparators: [',',' ','\n','\t']

    # Expands a given request type form fields
    changeType: (event) ->
        event.preventDefault() if event
        @$('#type .btn').removeClass 'active'
        @$("#type [data-type='#{ @requestType }']").addClass 'active'
        @$('.expandable').addClass 'hide'
        @$("##{ @requestType }-expandable").removeClass 'hide'
        @checkForm()

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

        @request = request
        @requestObject = requestObject

        @lockdown = true
        @$('button[type="submit"]').attr 'disabled', 'disabled'

        @saveRequest()

    afterRender: ->
        @renderFormElements()


module.exports = RequestFormNew
