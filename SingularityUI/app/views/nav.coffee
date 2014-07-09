View = require './view'

class NavView extends View

    template: require './templates/nav'

    events: ->
        _.extend super,
            'click [data-invoke-global-search]': 'showSearch'

    render: ->
        @setElement @template()
        $('body').prepend @$el

    showSearch: ->
        app.views.globalSearch.show()

module.exports = NavView
