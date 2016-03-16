React = require 'react'

Loader = React.createClass

  render: ->
    if @props.isVisable
      <div className="loading-spinner">
        <div className="loading">{@props.text}</div>
      </div>
    else
      <div></div>

module.exports = Loader
