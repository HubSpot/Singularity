View = require './view'

class NavigationView extends View

    el: '#top-level-nav'

    initialize: =>
        $('#top-level-nav').dblclick -> window.scrollTo 0, 0
        @theme = 'light'

    render: =>
        @renderTitle()
        @renderNavLinks()
        @collapse()

    renderTitle: =>
        subtitle = utils.getHTMLTitleFromHistoryFragment(Backbone.history.fragment)
        subtitle = ' — ' + subtitle if subtitle
        $('head title').text("Singularity#{ subtitle }")

    renderNavLinks: =>
        $nav = @$el

        @renderTheme @theme

        $anchors = $nav.find('ul.nav a[data-href]')
        $anchors.each ->
            route = $(@).data('href')
            $(@)
                .attr('href', "/#{ constants.appName }/#{ route }")
                .attr('data-route', route)

        $nav.find('li').removeClass('active')

        currentTopLevel = "/#{ constants.appName }/#{ Backbone.history.fragment.split('/')[0] }"

        $anchors.each ->
            if $(@).attr('href') in [currentTopLevel, currentTopLevel + 's']
                $(@).parents('li').addClass('active')

    renderTheme: (theme) =>
        previousTheme = if @theme is 'light' then 'dark' else 'light'
        $('html').addClass("#{ theme }strap").removeClass("#{ previousTheme }strap")
        $('#theme-changer').html(_.capitalize(previousTheme)).unbind('click').on 'click', =>
            newTheme = if @theme is 'dark' then 'light' else 'dark'
            @theme = newTheme
            @renderTheme @theme

    collapse: =>
        $collapse = $('#top-level-nav .nav-collapse')
        $collapse.collapse('hide') if $collapse.data().collapse

module.exports = NavigationView
