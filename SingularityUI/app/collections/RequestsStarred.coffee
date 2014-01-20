class RequestsStarred extends Backbone.Collection

    localStorage: new Backbone.LocalStorage('RequestNamesStarred')

    toggle: (requestName) ->
        model = @get(requestName)

        if model?
            model.destroy()
        else
            @create
                name: requestName
                added: + new Date()

module.exports = RequestsStarred