React = require 'react'
classNames = require 'classnames'

class LogLine extends React.Component
  @propTypes:
    offset: React.PropTypes.number.isRequired
    isHighlighted: React.PropTypes.bool.isRequired
    content: React.PropTypes.string.isRequired
    onPermalinkClick: React.PropTypes.func.isRequired
    permalinkEnabled: React.PropTypes.bool.isRequired
    search: React.PropTypes.string

  shouldComponentUpdate: (nextProps) ->
    (@props.offset isnt nextProps.offset) or
    (@props.isHighlighted isnt nextProps.isHighlighted) or
    (@props.search isnt nextProps.search)

  highlightContent: (content) ->
    search = @props.search
    if not search or _.isEmpty(search)
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

  handlePermalinkClick: (e) ->
    e.preventDefault()
    @props.onPermalinkClick(@props.offset)

  renderPermalink: ->
    if @props.permalinkEnabled
      <a href="##{@props.offset}" className="offset-link" onClick={@handlePermalinkClick}>
        <div className="pre-line">
            <span className="glyphicon glyphicon-link" data-offset="#{@props.offset}"></span>
        </div>
      </a>

  render: ->
    divClass = classNames
      line: true
      highlightLine: @props.isHighlighted

    <div className={divClass} style={backgroundColor: @props.color}>
      {@renderPermalink()}
      <span>
        {@props.offset} | 
        {@highlightContent(@props.content)}
      </span>
    </div>

module.exports = LogLine
