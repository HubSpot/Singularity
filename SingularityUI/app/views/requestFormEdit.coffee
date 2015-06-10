RequestFormBaseView = require './requestFormBase'

class RequestFormEdit extends RequestFormBaseView

    initialize: ->
        super
        app.$page.hide()

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
        @setTooltips()
        app.$page.show()

    setTooltips: ->
        @$("[data-tooltip='rack-sensitive']").tooltip
            title: 'Changes will only affect new tasks.'
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
