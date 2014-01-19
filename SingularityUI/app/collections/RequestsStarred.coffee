class RequestsStarred extends Backbone.Collection

    localStorage: new Backbone.LocalStorage('RequestsStarred')

    toggle: (requestId) ->
        model = @get(requestId)

        if model?
            model.destroy()
        else
            @create
                id: requestId
                added: + new Date()

module.exports = RequestsStarred