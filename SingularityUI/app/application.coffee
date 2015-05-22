Router = require 'router'

User = require 'models/User'

NavView = require 'views/nav'
GlobalSearchView = require 'views/globalSearch'

class Application

    # Holds `nav`, `globalSearch`, and `current`
    views: {}

    # Every @globalRefreshTime we send a refresh() request to teh active controller
    globalRefreshInterval: undefined
    globalRefreshTime:     60000 # one minute

    # Window becomes `blurred` if this page isn't active, e.g. the user
    # is on a different tab
    blurred: false

    initialize: ->
        @setupGlobalErrorHandling()

        @setupUser()

        @$page = $('#page')
        @page = @$page[0]

        $body = $ 'body'

        @views.nav = new NavView
        @views.nav.render()
        $body.prepend @views.nav.$el

        @views.globalSearch = new GlobalSearchView
        @views.globalSearch.render()
        $body.append @views.globalSearch.$el

        $('.page-loader.fixed').hide()

        @router = new Router

        # so sneaky
        el = document.createElement('a')
        el.href = config.appRoot

        Backbone.history.start
            pushState: true
            root: el.pathname

        # Global refresh
        @setRefreshInterval()

        # We don't want the refresh to trigger if the tab isn't active
        $(window).on 'blur',  =>
            @blurred = true
            clearInterval @globalRefreshInterval

        $(window).on 'focus', =>
            @blurred = false
            @globalRefresh()
            @setRefreshInterval()

    setRefreshInterval: ->
        clearInterval @globalRefreshInterval
        setInterval @globalRefresh, @globalRefreshTime

    globalRefresh: =>
        return if localStorage.getItem('suppressRefresh') is 'true'
        if @blurred
            clearInterval @globalRefreshInterval
            return
        @currentController.refresh()

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
            return if @blurred and jqxhr.statusText is 'timeout'

            url = settings.url.replace(config.appRoot, '')

            if jqxhr.status is 502
                Messenger().info
                    message:   "Singularity is deploying, your requests cannot be handled. Things should resolve in a few seconds so just hang tight!"
                    hideAfter: 10
            else if jqxhr.statusText is 'timeout'
                Messenger().error
                    message:   "<p>A <code>#{ jqxhr.statusText }</code> error occurred while accessing:</p><pre>#{ url }</pre>"
                    hideAFter: 20
            else if jqxhr.status is 0
                Messenger().error
                    message:   "<p>Could not reach the Singularity API. Please make sure SingularityUI is properly set up.</p><p>If running through Brunch, this might be your browser blocking cross-domain requests.</p>"
                    hideAfter: 20
            else
                console.log jqxhr.responseText
                try
                    serverMessage = JSON.parse(jqxhr.responseText).message or jqxhr.responseText
                catch
                    serverMessage = jqxhr.responseText

                serverMessage = _.escape serverMessage

                Messenger().error
                    message:   "<p>An uncaught error occurred with your request. The server said:</p><pre>#{ serverMessage }</pre><p>The error has been saved to your JS console.</p>"
                    hideAfter: 20

                console.error jqxhr
                throw new Error "AJAX Error"

    # Usually called by Controllers when they're initialized. Loader is overwritten by views
    showPageLoader: ->
        @$page.html "<div class='page-loader centered cushy'></div>"

    bootstrapController: (controller) ->
        @currentController = controller

    # Called by Controllers when their views are ready to take over
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

    getUsername: =>
        @user.get 'username'

module.exports = new Application
