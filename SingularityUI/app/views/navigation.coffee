View = require './view'

class NavigationView extends View

    el: '#top-level-nav'

    initialize: =>
        $('#top-level-nav').dblclick -> window.scrollTo 0, 0

    render: =>
        @renderTitle()
        @renderNavLinks()
        @collapse()
        @

    renderTitle: =>
        subtitle = utils.getHTMLTitleFromHistoryFragment(Backbone.history.fragment)
        subtitle = ' — ' + subtitle if subtitle
        $('head title').text("Singularity#{ subtitle }")

    renderNavLinks: =>
        $nav = @$el

        $anchors = $nav.find('ul.nav a[data-href]')
        $anchors.each ->
            route = $(@).data('href')
            $(@).attr('href', "#{ window.singularity.config.appRoot }/#{ route }")

        $nav.find('li').removeClass('active')

        currentTopLevel = "#{ window.singularity.config.appRoot }/#{ Backbone.history.fragment.split('/')[0] }"

        $anchors.each ->
            if $(@).attr('href') in [currentTopLevel, currentTopLevel + 's']
                $(@).parents('li').addClass('active')

    collapse: =>
        $collapse = $('#top-level-nav .nav-collapse')
        $collapse.collapse('hide') if $collapse.data().collapse

module.exports = NavigationView
