class RequestsStarred extends Backbone.Collection

    fetch: (params) ->
        @add JSON.parse localStorage.getItem 'starredRequests'

    saveState: ->
        localStorage.setItem 'starredRequests', JSON.stringify @models

    toggle: (requestName) ->
        model = @get requestName

        if model?
            @remove model
            @saveState()
        else
            @create
                id: requestName
                name: requestName
                added: + new Date()
            @saveState()


module.exports = RequestsStarred
