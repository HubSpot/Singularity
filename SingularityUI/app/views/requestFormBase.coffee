# Base View for creating and editing requests
# extended by requestFormEdit and requestFormNew

FormBaseView = require './formBaseView'

Request = require '../models/Request'

class RequestFormBase extends FormBaseView

    template: require '../templates/requestForm'

    # A list of request inputs that we want taggable
    taggables: ->
        type = @model?.get('request')?.requestType || @requestType
        racks = _.pluck @racks.toJSON(), 'id'
        [
            { 
                name: 'owners', selector: '#owners'
                options:
                    tags: true
                    containerCssClass: 'select-owners hide-select2-spinner'
                    dropdownCssClass: 'hidden'
            }
            { 
                name: 'rackAffinity', selector: "#rackAffinity-#{type}"
                options: tags: racks
            }
        ]

    events: ->
        _.extend super,
            'click #type .btn': 'handleChangeType'
            'change #schedule-type': 'handleScheduleTypeChange'

    initialize: ({@requestId, @racks}) ->
        super

    renderFormElements: ->
        for input in @taggables()
            options = $.extend({}, {tokenSeparators: [',',' ']}, input.options)
            @[input.name] = @$("#{input.selector}").select2 options

        @scheduleSelect = @generateSelectBox '#schedule-type', {containerCssClass : "select2-select-box select-box-small"}

    handleChangeType: (event) ->
        event.preventDefault() if event

    handleScheduleTypeChange: (event) ->
        placeholderText =
            schedule: "eg: */5 * * * *"
            quartzSchedule: "eg: 0 */5 * * * ?"
        
        type = @$('#schedule-type').val()

        @$('#schedule').attr("placeholder", placeholderText[type])

    # Expands a given request type's form fields
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
        @$("[data-alert-location='form']").remove()

        requestObject = {}

        requestObject.id = @model?.id or @$('#id').val()

        requestObject.requestType = @$('#type .active').data 'type'
        type = requestObject.requestType

        requestObject.owners = @getSelect2Val '#owners'

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

            requestObject.hideEvenNumberAcrossRacksHint = @$("#hide-distribute-across-racks-hint-#{ type }").is ':checked'
            
            requestObject.rackAffinity = @getSelect2Val "#rackAffinity-#{ type }"

        if type in ['SCHEDULED', 'ON_DEMAND', 'RUN_ONCE']
            killOld = parseInt @$("#killOldNRL-#{ type }").val()
            requestObject.killOldNonLongRunningTasksAfterMillis = killOld if killOld

        if type in ['ON_DEMAND', 'RUN_ONCE']
            requestObject.daemon = false

        if type is 'SCHEDULED'
            schedule = @$('#schedule').val()
            scheduleType = @$('#schedule-type').val()
            retries  = parseInt @$('#retries-on-failure').val()
            expectedRuntime = parseInt @$('#scheduled-expected-runtime').val()

            requestObject[scheduleType] = schedule if schedule
            if scheduleType is 'quartzSchedule'
                requestObject.schedule = ''

            requestObject.numRetriesOnFailure = retries if retries
            requestObject.scheduledExpectedRuntimeMillis = expectedRuntime if expectedRuntime

        if type is 'SERVICE'
            requestObject.loadBalanced  = @$('#load-balanced').is ':checked'

        @request = new Request requestObject
        @request.raw = true
        @request.url = "#{ config.apiRoot }/requests"
        @request.isNew = -> true

        @requestObject = requestObject

        @lockdown = true
        @$('button[type="submit"]').attr 'disabled', 'disabled'

        @saveRequest()

    afterRender: ->
        @renderFormElements()


module.exports = RequestFormBase
