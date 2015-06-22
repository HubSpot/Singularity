RequestFormBaseView = require './requestFormBase'

class RequestFormEdit extends RequestFormBaseView

    initialize: ->
        super
        app.$page.hide()

    events: ->
        _.extend super,
            'change #schedule-type': 'handleScheduleTypeChange'

    handleScheduleTypeChange: (e) ->
        scheduleType = $(e.currentTarget).val()
        @$("#schedule").val @model.get('request')[scheduleType]

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
           
        if request.type in ['SCHEDULED','ON_DEMAND','RUN_ONCE']
            @$("#killOldNRL-#{request.type}").val request.request.killOldNonLongRunningTasksAfterMillis

        if request.type is 'WORKER'
            @$("#waitAtLeast-#{request.type}").val request.request.waitAtLeastMillisAfterTaskFinishesForReschedule

        if request.type is 'SCHEDULED'
            @$("#schedule").val request.request.quartzSchedule
            @setSelect2Val '#schedule-type', 'quartzSchedule'

            @$("#retries-on-failure").val request.request.numRetriesOnFailure
            @$("#scheduled-expected-runtime").val request.request.scheduledExpectedRuntimeMillis

        typeButtons = @$('#type .btn').prop('disabled', true)
        @$("[data-type='#{request.type}']").prop('disabled', false)
        @setTooltips()
        app.$page.show()

    setTooltips: ->
        @$("[data-tooltip='cannot-change']").tooltip
            title: 'Option cannot be altered after creation'
            placement: 'top'

    saveRequest: ->
        request = @model.toJSON()

        if _.contains ['RUN_ONCE', 'ON_DEMAND'], request.type
            @model.unset 'instances'
        
        @model.url = "#{ config.apiRoot }/requests?user=#{ app.getUsername() }"
        @model.isNew = -> true

        serverRequest = @model.save @requestObject

        serverRequest.done  (response) =>
            @lockdown = false
            @alert "Your Request <a href='#{ config.appRoot }/request/#{ response.id }'>#{ response.id }</a> has been updated"

        serverRequest.error (response) =>
            @postSave()

            app.caughtError()
            @alert "There was a problem saving your request. The server response has been logged to your JS console.", false
            console.error response


module.exports = RequestFormEdit
