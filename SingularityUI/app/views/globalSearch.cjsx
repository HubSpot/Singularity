View = require './view'

Requests = require '../collections/Requests'

GlobalSearch = require '../components/globalSearch/GlobalSearch'

React = require 'react'
ReactDOM = require 'react-dom'

class GlobalSearchView extends View
  show: =>
    @searchActive = true
    @collection.fetch().done =>
      @render()

    @render()
    @focusSearch()

  hide: =>
    @searchActive = false
    @render()

  focusSearch: =>
    # TODO: when this class get's Reactified as well, use refs
    $('input.big-search-box').focus()

  searchActive: false

  initialize: ({@state}) ->
    @collection = new Requests [], state: 'all'

    $(window).on 'keydown', (event) =>
      focusBody = $(event.target).is 'body'
      focusInput = $(event.target).is @$ 'input.big-search-box'

      modifierKey = event.metaKey or event.shiftKey or event.ctrlKey
      sPressed = event.keyCode in [83, 84] and not modifierKey
      escPressed = event.keyCode is 27

      if escPressed and (focusBody or focusInput)
        @hide()
      else if sPressed and focusBody
        @show()
        event.preventDefault()

  render: ->
    ReactDOM.render(
      <GlobalSearch
        requests=@collection
        visible={@searchActive}
        onHide=@hide
      />,
      @el)

module.exports = GlobalSearchView
