View = require './view'

class NavigationView extends View

    el: '#top-level-nav'

    initialize: =>
        $('#top-level-nav').dblclick -> window.scrollTo 0, 0

        app.user_settings.on 'change:theme', => @render()

    render: =>
        @renderTitle()
        @renderNavLinks()
        @collapse()

    renderTitle: =>
        subtitle = utils.getHTMLTitleFromHistoryFragment(Backbone.history.fragment)
        subtitle = ' — ' + subtitle if subtitle isnt ''
        $('head title').text("Singularity#{subtitle}")

    renderNavLinks: =>
        $nav = @$el

        @renderTheme app.user_settings.get('theme')

        $anchors = $nav.find('ul.nav a:not(".dont-route")')
        $anchors.each ->
            route = $(@).data('href')
            $(@)
                .attr('href', "/#{constants.app_name}/#{route}")
                .data('route', route)

        $nav.find('li').removeClass('active')
        $anchors.each ->
            $(@).parents('li').addClass('active') if $(@).attr('href') is "/#{constants.app_name}/#{Backbone.history.fragment}"

    renderTheme: (theme) =>
        settings = app.user_settings
        previous_theme = if settings.get('theme') is 'light' then 'dark' else 'light'
        $('html').addClass("#{theme}strap").removeClass("#{previous_theme}strap")
        $('#theme-changer').html(_.capitalize(previous_theme)).unbind('click').click ->
            new_theme = if settings.get('theme') is 'dark' then 'light' else 'dark'
            app.user_settings.set(theme: new_theme).save()

    collapse: =>
        $collapse = $('#top-level-nav .nav-collapse')
        $collapse.collapse('hide') if $collapse.data().collapse

module.exports = NavigationView