View = require './view'

TasksSearch = require '../collections/TasksSearch'
RequestsSearch = require '../collections/RequestsSearch'

class SearchView extends View

    template: require './templates/search'
    templateResults: require './templates/searchResults'

    initialize: ->
        @forceSearchOnce = false

        @currentSearchOptions =
            requests:
                orderBy: 'createdAt'
                orderDirection: 'DESC'
            tasks:
                orderBy: 'updatedAt'
                orderDirection: 'DESC'

    render: ->
        @$el.html @template

        @$searchOptions = @$ '.search-options'
        @$search = @$ 'input[type="search"]'

        @setupSearchOptions()
        @setUpSearchEvents()

        @

    setupSearchOptions: ->
        @$searchOptions.find('select').each (i, select) =>
            $select = $ select
            selectDrop = new Select el: select

            option = $select.attr('data-search-option')
            category = $select.attr('data-search-category')

            $select.val @currentSearchOptions[category][option]
            $select.change()

            $select.on 'change', =>
                @currentSearchOptions[category][option] = $select.val()

                @forceSearchOnce = true
                @$search.click()

    renderResults: ->
        context =
            tasksResults: _.pluck(@tasksResults.sort().models, 'attributes').reverse()
            requestsResults: _.pluck(@requestsResults.sort().models, 'attributes').reverse()

        @$el.find('.results').html @templateResults context

    setupEvents: ->
        @$el.find('[data-action="viewJSON"]').unbind('click').on 'click', (e) ->
            utils.viewJSON 'task', $(e.target).data('task-id')

    setUpSearchEvents: ->
        if not app.isMobile
            setTimeout => @$search.focus()

        lastText = ''
        minimumSearchQuery = 8
        showSpinnerTimeout = undefined
        showSlowSearchAPITimeout = undefined

        @$search.unbind().on 'change keypress paste focus textInput input click keydown', _.debounce =>
            searchText = _.trim @$search.val()

            if searchText.length < minimumSearchQuery
                @$el.find('.results').html "<br><br><center>Please type at least #{ minimumSearchQuery } characters to execute a search.</center>"
                return

            if @forceSearchOnce or searchText isnt lastText and searchText.length
                if @lastXhrTasks?
                   @lastXhrTasks.abort()
                   @lastXhrRequests.abort()
                   clearTimeout showSpinnerTimeout
                   clearTimeout showSlowSearchAPITimeout

                @forceSearchOnce = false
                lastText = searchText

                @requestsResults = new RequestsSearch [], { query: searchText, params: @currentSearchOptions['requests'] }
                @lastXhrRequests = @requestsResults.fetch()

                @tasksResults = new TasksSearch [], { query: searchText, params: @currentSearchOptions['tasks'] }
                @lastXhrTasks = @tasksResults.fetch()

                showSpinnerTimeout = setTimeout =>
                    @$el.find('.results').html '<br><br><div class="page-loader centered"></div>'
                , 500

                showSlowSearchAPITimeout = setTimeout =>
                    @$el.find('.results').html '<br><br><div class="page-loader centered"></div><br><br><center>Sorry the search API is <a href="https://github.com/HubSpot/Singularity/issues/90" target="_blank">so slow</a>...</center>'
                , 2000

                $.when(@lastXhrTasks, @lastXhrRequests).done =>
                    clearTimeout showSpinnerTimeout
                    clearTimeout showSlowSearchAPITimeout
                    @renderResults()

        , 35

module.exports = SearchView