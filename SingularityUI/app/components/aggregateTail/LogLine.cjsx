React = require 'react'
classNames = require 'classnames'

LogLine = React.createClass

  getClassNames: ->
    clazz = 'line'
    clazz += if @props.highlighted then ' highlightLine' else ''
    clazz += if @props.index is 0 then ' first-line' else ''
    clazz += if @props.index >= @props.totalLines - 1 then ' last-line' else ''
    clazz

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
      <span key={i} className={if s.match then 'search-match'}>{s.text}</span>

  render: ->
    <div className="#{@getClassNames()}" style={backgroundColor: @props.color}>
      <a target="blank" href="#{@props.offsetLink}" className="offset-link" data-offset="#{@props.offset}">
        <div className="pre-line">
            <span className="glyphicon glyphicon-link" data-offset="#{@props.offset}"></span>
        </div>
      </a>
      <span>
        {@highlightContent(@props.content)}
      </span>
    </div>

module.exports = LogLine
