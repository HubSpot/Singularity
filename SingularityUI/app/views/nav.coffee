View = require './view'

class NavView extends View

    template: require './templates/nav'

    events: ->
        _.extend super,
            'click [data-invoke-global-search]': 'showSearch'

    render: ->
        fragment = Backbone.history.fragment?.split("/")[0]

        # If we haven't attached it to the body yet
        if @$el?.parent().length is 0
            @setElement @template {fragment}

            $('body').prepend @$el
        else
            @$el.html $(@template {fragment}).html()

    showSearch: ->
        app.views.globalSearch.show()

module.exports = NavView
