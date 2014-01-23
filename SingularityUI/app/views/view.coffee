require 'lib/view_helper'

# Base class for all views.
class View extends Backbone.View

    events:
        'click a[data-route]': 'routeLink'

    routeLink: (e) =>
        $link = $(e.target)

        $parentLink = $link.parents('a[href]')
        $link = $parentLink if $parentLink.length

        url = $link.attr('href')

        return true if $link.attr('target') is '_blank' or url is 'javascript:;' or typeof url is 'undefined' or url.substr(0, 4) is 'http'

        if e.metaKey or e.ctrlKey or e.shiftKey
            return

        e.preventDefault()

        if url.indexOf('.') == 0
            url = url.substring 1

            if url.indexOf('/') == 0
                url = url.substring 1

        url = $link.data('route') if $link.data('route') or $link.data('route') is ''

        app.router.navigate url, trigger: true

module.exports = View
