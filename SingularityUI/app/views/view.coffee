require 'lib/view_helper'

class View extends Backbone.View
    events: ->
        'click a': 'routeLink'

    routeLink: (e) =>
        $link = $(e.target)

        $parentLink = $link.parents('a[href]')
        $link = $parentLink if $parentLink.length

        url = $link.attr('href')

        return true if $link.attr('target') is '_blank' or url is 'javascript:;' or typeof url is 'undefined' or url.indexOf(config.appRoot) != 0

        if e.metaKey or e.ctrlKey or e.shiftKey
            return

        e.preventDefault()

        url = url.replace(config.appRoot, '')

        app.router.navigate url, trigger: true

module.exports = View
