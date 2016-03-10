View = require './view'

class SimpleSubview extends View

    currentPage: 0

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
        metadataElements = @getMetadataElementsOfCategory()
        metadataElementsOnPage = @getMetadataElementsOnPage metadataElements
        data =
            config:    config
            synced:    @model.synced
            metadataElementToDisplay: metadataElementsOnPage
            alertClass: @params.alertClass
            currentPage: @currentPage
            isNotFirstPage: @currentPage > 0
            isNotLastPage: @getUpperBound() < metadataElements.length - 1

        data

    metadataElementCategoryIs: (metadataElement, category) ->
        return true

    getLowerBound: () ->
        @currentPage * @numberPerPage

    getUpperBound: () ->
        if @currentPage is 0
            @numberPerPage - 1 #otherwise you multiply by 0, subtract 1, and get -1
        else
            @currentPage * (@numberPerPage + 1) - 1

    getMetadataElementsOfCategory: () ->
        metadataElements = @model.attributes.taskMetadata.filter (metadataElement) =>
            @metadataElementCategoryIs metadataElement, @params.category

    getMetadataElementsOnPage: (metadataElements) ->
        if @getUpperBound() >= metadataElements.length
            metadataElements[@getLowerBound()..]
        else
            metadataElements[@getLowerBound()..@getUpperBound()] 

    increasePage: (event) ->
        event.preventDefault()
        @currentPage = @currentPage + 1 unless @currentPage + 1 >= @getMetadataElementsOfCategory().length
        @render()

    decreasePage: (event) ->
        event.preventDefault()
        @currentPage-- unless @currentPage < 1
        @render()

module.exports = SimpleSubview
