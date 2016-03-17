Router = require 'router'

User = require 'models/User'

NavView = require 'views/nav'
GlobalSearchView = require 'views/globalSearch'

Sortable = require 'sortable'

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
        el.href = config.appRoot or '/'

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
            else if jqxhr.status is 401 and config.redirectOnUnauthorizedURL
                window.location.href = config.redirectOnUnauthorizedURL.replace('{URL}', encodeURIComponent(window.location.href))
            else if jqxhr.statusText is 'timeout'
                Messenger().error
                    message:   "<p>A <code>#{ jqxhr.statusText }</code> error occurred while accessing:</p><pre>#{ url }</pre>"
            else if jqxhr.status is 0
                Messenger().error
                    message:   "<p>Could not reach the Singularity API. Please make sure SingularityUI is properly set up.</p><p>If running through locally, this might be your browser blocking cross-domain requests.</p>"
            else
                try
                    serverMessage = JSON.parse(jqxhr.responseText).message or jqxhr.responseText
                catch
                    if jqxhr.status is 200
                        console.error jqxhr.responseText
                        Messenger().error
                            message:    """
                                            <p>Expected JSON but received #{if jqxhr.responseText.startsWith '<!DOCTYPE html>' then 'html' else 'something else'}. The response has been saved to your js console.</p>
                                        """
                        throw new Error "Expected JSON in response but received #{if jqxhr.responseText.startsWith '<!DOCTYPE html>' then 'html' else 'something else'}"
                    serverMessage = jqxhr.responseText

                serverMessage = _.escape serverMessage
                id = "message_" + Date.now()
                selector = "##{id}"

                Messenger().error
                    message:   """<div id="#{id}">
                                    <p>An uncaught error occurred with your request. The server said:</p>
                                    <pre class="copy-text">#{ serverMessage }</pre>
                                    <p>The error has been saved to your JS console. <span class='copy-link'>Copy error message</span>.</p>
                                </div>"""
                console.error jqxhr
                options =
                    selector: selector
                    linkText: 'Copy error message'
                    copyLink: '.copy-link'

                utils.makeMeCopy(options)
                throw new Error "AJAX Error"

    # Usually called by Controllers when they're initialized. Loader is overwritten by views
    showPageLoader: ->
        @$page.html "<div class='page-loader centered cushy'></div>"

    showFixedPageLoader: ->
        @$page.append "<div class='page-loader page-loader-fixed'></div>"

    hideFixedPageLoader: ->
        @$page.find('.page-loader-fixed').remove()

    bootstrapController: (controller) ->
        @currentController = controller

    # Called by Controllers when their views are ready to take over
    showView: (view) ->
        # Fire a view change event for manual cleanups (Unmount react components)
        window.dispatchEvent(new Event('viewChange'));

        # Clean up events & stuff
        @views.current?.remove()

        $(window).scrollTop 0

        @views.current = view
        # Render & display the view
        view.render()

        if @page.children.length
            @page.replaceChild view.el, @page.children[0]
            Sortable.init()
        else
            @page.appendChild view.el

    setupUser: ->
        @user = new User
        @user.fetch() # Syncronous because it uses localStorage

    getUsername: =>
        if @user.get('authenticated')
            @user.get('user').id
        else
            ''

module.exports = new Application
