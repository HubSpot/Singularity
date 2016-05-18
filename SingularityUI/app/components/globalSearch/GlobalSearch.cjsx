React = require 'react'
classNames = require 'classnames'

{ Typeahead } = require 'react-typeahead'
fuzzy = require 'fuzzy'

class GlobalSearch extends React.Component
  optionSelected: (requestIdObject) =>
    requestId = @getValueFromOption(requestIdObject)
    app.router.navigate "/request/#{ requestId }", { trigger: true }
    @clear()
    @props.onHide()

  resetSelection: =>
    @refs.typeahead.setState({
      selectionIndex: 0
    })

  clear: =>
    @refs.typeahead.setEntryText('')
    @resetSelection()

  focus: =>
    @refs.typeahead.focus()
    @resetSelection()

  searchOptions: (inputValue, options) ->
    # fuzzy lazily just appends a string before and after a matching char
    # we have to later use a simple shift-in shift-out state machine to convert
    fuzzyOptions = {
      returnMatchInfo: true
    }

    searched = fuzzy.filter(inputValue, options, fuzzyOptions)

    return searched

  renderOption: (option, index) ->
    # transform fuzzy string into react component
    bolded = option.string.map((matchInfo) ->
      if matchInfo.match then <b>{matchInfo.char}</b> else <span>{matchInfo.char}</span>
    )

    return bolded

  getValueFromOption: (option) ->
    return option.original

  render: =>
    if @props.visible
      @focus()

    options = _.pluck(@props.requests.toJSON(), 'id')

    globalSearchClasses = classNames
      'global-search': true
      'global-search-active': @props.visible

    <div className={globalSearchClasses}>
      <div className='container'>
        <div className='close-button-container'>
          <a onClick={@props.onHide}>&times;</a>
        </div>

        <p className='hidden-xs text-muted tip'>
          Protip: You can press <kbd>s</kbd> or <kbd>t</kbd> to open global search and <kbd>esc</kbd> to close it.
        </p>
        <Typeahead
          ref='typeahead'
          options=options
          maxVisible={10}
          customClasses={{
            input: 'big-search-box'
          }}
          placeholder='Search all requests'
          onOptionSelected=@optionSelected
          searchOptions=@searchOptions
          displayOption=@renderOption
          formInputOption=@getValueFromOption
          inputDisplayOption=@getValueFromOption
        />
      </div>
    </div>

module.exports = GlobalSearch
