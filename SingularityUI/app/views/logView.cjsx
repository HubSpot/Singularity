View = require './view'
React = require 'react'
ReactDOM = require 'react-dom'
LogContainer = require '../components/logs/LogContainer'
{ Provider } = require 'react-redux'

class LogView extends View
    initialize: (store) ->
      window.addEventListener 'viewChange', @handleViewChange
      @component = <Provider store={store}><LogContainer /></Provider>

    handleViewChange: =>
      unmounted = ReactDOM.unmountComponentAtNode @el
      if unmounted
        window.removeEventListener 'viewChange', @handleViewChange

    render: ->
      $(@el).addClass 'tail-root'
      ReactDOM.render @component, @el

module.exports = LogView
