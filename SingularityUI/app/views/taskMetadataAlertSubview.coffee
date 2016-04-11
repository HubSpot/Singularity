View = require './view'

class SimpleSubview extends View

    currentPage: 1

    events: ->
        _.extend super,
            'click [data-action="increase-page"]': 'increasePage'
            'click [data-action="decrease-page"]': 'decreasePage'

    initialize: (@params) ->
        { @template } = @params
        @numberPerPage = @params.numberPerPage or 1

        for eventName in ['sync', 'add', 'remove', 'change', 'reset']
            @listenTo @model, eventName, @render

    render: ->
        return if not @model.synced and @model.isEmpty?()

        @$el.html @template(@renderData())

        super.afterRender()

    renderData: ->
        metadataElements = @getMetadataElementsOfLevel()
        metadataElementsOnPage = @getMetadataElementsOnPage metadataElements
        data =
            config:    config
            synced:    @model.synced
            metadataElementToDisplay: metadataElementsOnPage
            alertClass: @params.alertClass
            currentPage: @currentPage
            isNotFirstPage: @currentPage > 1
            isNotLastPage: @getUpperBound() < metadataElements.length - 1

        data

    getLowerBound: () ->
        (@currentPage - 1) * @numberPerPage

    getUpperBound: () ->
        @currentPage * @numberPerPage - 1

    getMetadataElementsOfLevel: () ->
        metadataElements = @model.attributes.taskMetadata.filter (metadataElement) =>
            metadataElement.level is @params.level

    getMetadataElementsOnPage: (metadataElements) ->
        if @getUpperBound() >= metadataElements.length
            metadataElements[@getLowerBound()..]
        else
            metadataElements[@getLowerBound()..@getUpperBound()] 

    increasePage: (event) ->
        event.preventDefault()
        @currentPage = @currentPage + 1 unless @getUpperBound() >= @getMetadataElementsOfLevel().length - 1
        @render()

    decreasePage: (event) ->
        event.preventDefault()
        @currentPage-- unless @currentPage < 2
        @render()

module.exports = SimpleSubview
