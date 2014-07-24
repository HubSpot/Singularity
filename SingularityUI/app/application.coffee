Router = require 'lib/router'

User = require 'models/User'

NavView = require 'views/nav'
GlobalSearchView = require 'views/globalSearch'

class Application

    views: {}

    initialize: ->
        @isMobile = touchDevice = 'ontouchstart' of document.documentElement
        @setupGlobalErrorHandling()

        @setupUser()

        @$page = $('#page')
        @page = @$page[0]

        @setupNav()
        @setupGlobalSearchView()

        $('.page-loader.fixed').hide()

        @router = new Router

        # so sneaky
        el = document.createElement('a')
        el.href = config.appRoot

        Backbone.history.start
            pushState: true
            root: el.pathname

    caughtError: ->
        # Ghetto try-catch
        #
        # If there's some sort of AJAX error we can choose to handle this ourselves
        # If we do handle it, we can call app.caughtError() in that bit of code
        # and it'll prevent the default error message from being displayed,
        # e.g. `model.fetch().error => app.caughtError()`
        @caughtThisError = true

    setupGlobalErrorHandling: ->
        unloading = false
        $(window).on 'beforeunload', ->
            unloading = true
            return

        blurred = false
        $(window).on 'blur',  -> blurred = true
        $(window).on 'focus', -> blurred = false

        # When an Ajax error occurs this is the default message that is displayed.
        # You can add your own custom error handling using app.caughtError() above.
        $(document).on 'ajaxError', (e, jqxhr, settings) =>
            # If we handled this already, ignore it
            if @caughtThisError
                @caughtThisError = false
                return

            return if settings.suppressErrors
            return if jqxhr.statusText is 'abort'
            return if unloading
            return if blurred and jqxhr.statusText is 'timeout'

            url = settings.url.replace(config.appRoot, '')

            if jqxhr.status is 502
                Messenger().post "<p>A request failed because Singularity is deploying. Things should resolve in a few seconds so just hang tight...</p>"
            else if jqxhr.statusText is 'timeout'
                Messenger().post "<p>A <code>#{ jqxhr.statusText }</code> error occurred while accessing:</p><pre>#{ url }</pre>"
            else
                console.error jqxhr
                throw new Error "AJAX Error"
                Messenger().post "<p>An error occurred when trying to access:</p><pre>#{ url }</pre><p>Check JS console for response.</p>"
                
    # Called in Router. Shows the passed view's $el on the page
    show: (view) ->
        if @page.children.length
            @page.replaceChild view.el, @page.children[0]
        else
            @page.appendChild view.el

    bootstrapController: (controller) ->
        controller.initialize()
        @showView controller.view

    showView: (view) ->
        # Clean up events & stuff
        @views.current?.remove()

        $(window).scrollTop 0

        @views.current = view
        # Render & display the view
        view.render()
        if @page.children.length
            @page.replaceChild view.el, @page.children[0]
        else
            @page.appendChild view.el

    setupUser: ->
        @user = new User
        @user.fetch() # Syncronous because it uses localStorage

        if not @user.get('deployUser')
            Backbone.history.once 'route', =>
                setTimeout (=> @deployUserPrompt()), 1000

    getUsername: =>
        @user.get "deployUser" or "Unknown"

    deployUserPrompt: (welcome) ->
        vex.dialog.prompt
            message: require('templates/vex/usernamePrompt')()
            value: @user.get('deployUser')
            placeholder: 'user'
            callback: (user) =>
                if _.isString(user) and user isnt ''
                    @user.set('deployUser', @user.deployUser = user)
                    @user.save()

    setupNav: ->
        @views.nav = new NavView
        @views.nav.render()

    setupGlobalSearchView: ->
        @views.globalSearch = new GlobalSearchView
        @views.globalSearch.render()

module.exports = new Application
