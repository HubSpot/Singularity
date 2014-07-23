class RequestsStarred extends Backbone.Collection

    localStorage: new Backbone.LocalStorage "RequestsStarred"

    toggle: (requestName) ->
        model = @get requestName

        if model?
            model.destroy()
        else
            @create
                id: requestName
                name: requestName
                added: + new Date()

module.exports = RequestsStarred
