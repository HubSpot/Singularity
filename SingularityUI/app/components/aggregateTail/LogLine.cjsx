
LogLine = React.createClass

  shouldComponentUpdate: (nextProps) ->
    (@props.offset isnt nextProps.offset) or (@props.isHighlighted isnt nextProps.isHighlighted) or (@props.content isnt nextProps.content)

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

  handleClick: (e) ->
    e.preventDefault()
    window.history.pushState({}, window.document.title, @props.offsetLink)  # have to do it this janky way because of the hash
    @props.handleOffsetLink(@props.offset)

  render: ->
    divClass = classNames
      line: true
      highlightLine: @props.isHighlighted
      'first-line': @props.index is 0
      'last-line': @props.index >= @props.totalLines - 1

    <div className={divClass} style={backgroundColor: @props.color}>
      <a href="#{@props.offsetLink}" className="offset-link" data-offset="#{@props.offset}" onClick={@handleClick}>
        <div className="pre-line">
            <span className="glyphicon glyphicon-link" data-offset="#{@props.offset}"></span>
        </div>
      </a>
      <span>
        {@highlightContent(@props.content)}
      </span>
    </div>

module.exports = LogLine
