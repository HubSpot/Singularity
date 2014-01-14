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

        @$searchOptions = $ '.search-options'
        @$search = @$el.find('input[type="search"]')

        @setupSearchOptions()
        @setUpSearchEvents()

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
        @$search.focus() if $(window).width() > 568

        lastText = _.trim @$search.val()

        @$search.on 'change keypress paste focus textInput input click keydown', _.debounce =>
            text = _.trim @$search.val()

            if @forceSearchOnce or text isnt lastText and text.length
                if @lastXhrTasks?
                   @lastXhrTasks.abort()
                   @lastXhrRequests.abort()

                @forceSearchOnce = false
                lastText = text

                @requestsResults = new RequestsSearch [], { query: text, params: @currentSearchOptions['requests'] }
                @lastXhrRequests = @requestsResults.fetch()

                @tasksResults = new TasksSearch [], { query: text, params: @currentSearchOptions['tasks'] }
                @lastXhrTasks = @tasksResults.fetch()

                $.when(@lastXhrTasks, @lastXhrRequests).done => @renderResults()

        , 35

module.exports = SearchView