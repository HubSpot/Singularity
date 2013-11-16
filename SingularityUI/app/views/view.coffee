require 'lib/view_helper'

# Base class for all views.
class View extends Backbone.View

    el: '#page'

    events:
        'click a': 'routeLink'

    routeLink: (e) =>
        $link = $(e.target)

        url = $link.attr('href')

        return true if $link.attr('target') is '_blank' or typeof url is 'undefined' or url.substr(0, 4) is 'http'

        e.preventDefault()

        if url.indexOf('.') == 0
            url = url.substring 1

            if url.indexOf('/') == 0
                url = url.substring 1

        url = $link.data('route') if $link.data('route') or $link.data('route') is ''

        app.router.navigate url, trigger: true

module.exports = View