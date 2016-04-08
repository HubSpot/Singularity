React = require 'react'
classNames = require 'classnames'

{ connect } = require 'react-redux'
{ clickPermalink } = require '../../actions/log'

class LogLine extends React.Component
  @propTypes:
    offset: React.PropTypes.number.isRequired
    isHighlighted: React.PropTypes.bool.isRequired
    content: React.PropTypes.string.isRequired
    taskId: React.PropTypes.string.isRequired
    showDebugInfo: React.PropTypes.bool
    color: React.PropTypes.string

    search: React.PropTypes.string
    clickPermalink: React.PropTypes.func.isRequired

  highlightContent: (content) ->
    search = @props.search
    if not search or _.isEmpty(search)
      if @props.showDebugInfo
        return "#{ @props.offset } | #{ @props.timestamp } | #{ content }"
      else
        return content

    regex = RegExp(search, 'g')
    matches = []

    while m = regex.exec(content)
      matches.push(m)

    sections = []
    lastEnd = 0
    for m in matches
      last =
        text: content.slice(lastEnd, m.index)
        match: false
      sect =
        text: content.slice(m.index, m.index + m[0].length)
        match: true
      sections.push last, sect
      lastEnd = m.index + m[0].length
    sections.push
      text: content.slice(lastEnd)
      match: false

    sections.map (s, i) =>
      spanClass = classNames
        'search-match': s.match
      <span key={i} className={spanClass}>{s.text}</span>

  render: ->
    divClass = classNames
      line: true
      highlightLine: @props.isHighlighted

    <div className={divClass} style={backgroundColor: @props.color}>
      <a href="##{@props.offset}" className="offset-link" onClick={=> @props.clickPermalink(@props.offset)}>
        <div className="pre-line">
            <span className="glyphicon glyphicon-link" data-offset="#{@props.offset}"></span>
        </div>
      </a>
      <span>
        {@highlightContent(@props.content)}
      </span>
    </div>

mapStateToProps = (state, ownProps) ->
  search: state.search
  showDebugInfo: state.showDebugInfo

mapDispatchToProps = { clickPermalink }

module.exports = connect(mapStateToProps, mapDispatchToProps)(LogLine)
