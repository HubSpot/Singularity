React = require 'react'
classNames = require 'classnames'

class LoadingSpinner extends React.Component
  @propTypes:
    text: React.PropTypes.string
    centered: React.PropTypes.bool

  render: ->
    className = classNames
      'page-loader': true
      centered: @props.centered

    if @props.children.length > 0
      <div className="page-loader-with-message">
        <div className={className} />
        <p>{ @props.children }</p>
      </div>
    else
      <div className={className} />

module.exports = LoadingSpinner