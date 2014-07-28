View = require './view'

class NotFoundView extends View

    template: require '../templates/404'

    render: =>
        @$el.html @template fragment: Backbone.history.fragment

module.exports = NotFoundView
