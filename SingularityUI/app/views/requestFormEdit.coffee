RequestFormBaseView = require './requestFormBase'

class RequestFormEdit extends RequestFormBaseView

    initialize: ->
        super
        app.$page.hide()

    handleScheduleTypeChange: (e) ->
        super
        scheduleType = $(e.currentTarget).val()
        @$("#schedule").val @model.get('request')[scheduleType]

    renderEditForm: ->
        request = @model.toJSON()
        @requestType = request.request.requestType

        @render()
        # expand appropriate form fields
        @changeType()

        # render taggable data
        for tag in @taggables()
            @renderTaggable @[tag.name], request.request[tag.name]

        @$('#slavePlacement').val request.request.slavePlacement

        if @requestType is 'SERVICE' or 'WORKER'
            @$("#instances-#{@requestType}").val request.request.instances
            @$("#rack-sensitive-#{@requestType}").prop 'checked', request.request.rackSensitive
            @$("#hide-distribute-across-racks-hint-#{@requestType}").prop 'checked', request.request.hideEvenNumberAcrossRacksHint
            @$("#load-balanced").prop 'checked', request.request.loadBalanced

        if @requestType in ['SCHEDULED','ON_DEMAND','RUN_ONCE']
            @$("#killOldNRL-#{@requestType}").val request.request.killOldNonLongRunningTasksAfterMillis

        if @requestType is 'WORKER'
            @$("#waitAtLeast-#{@requestType}").val request.request.waitAtLeastMillisAfterTaskFinishesForReschedule

        if @requestType is 'SCHEDULED'
            @$("#schedule").val request.request.quartzSchedule
            @setSelect2Val '#schedule-type', 'quartzSchedule'

            @$("#retries-on-failure").val request.request.numRetriesOnFailure
            @$("#scheduled-expected-runtime").val request.request.scheduledExpectedRuntimeMillis

        typeButtons = @$('#type .btn').prop('disabled', true)
        @$("[data-type='#{@requestType}']").prop('disabled', false)
        @$("[data-type='#{@requestType}']").attr('data-tooltip', 'cannot-change')
        @setTooltips()
        app.$page.show()

    setTooltips: ->
        @$("[data-tooltip='cannot-change']").tooltip
            title: 'Option cannot be altered after creation'
            placement: 'top'
            container: 'body'

    saveRequest: ->
        if _.contains ['RUN_ONCE', 'ON_DEMAND'], @requestType
            @model.unset 'instances'

        @model.url = "#{ config.apiRoot }/requests"
        @model.isNew = -> true

        serverRequest = @model.save @requestObject

        serverRequest.done  (response) =>
            @lockdown = false
            @alert "Your Request <a href='#{ config.appRoot }/request/#{ response.id }'>#{ response.id }</a> has been updated"
            Backbone.history.navigate "/request/#{ response.id }", {trigger: true}

        serverRequest.error (response) =>
            @postSave()

            app.caughtError()
            @alert "There was a problem saving your request: #{ response.responseText }", false


module.exports = RequestFormEdit
