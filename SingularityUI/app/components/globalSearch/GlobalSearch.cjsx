React = require 'react'

{ Typeahead } = require 'react-typeahead'


class GlobalSearch extends React.Component
  optionSelected: (requestId) =>
    app.router.navigate "/request/#{ requestId }", { trigger: true }
    @clear()
    @props.onHide()

  clear: =>
    @refs.typeahead.setEntryText('')
    @refs.typeahead._onEscape() # hack to clear search index, TODO: PR react-typeahead

  focus: =>
    @refs.typeahead.focus()

  render: =>
    if @props.visible
      @focus()

    options = _.pluck(@props.requests.toJSON(), 'id')

    globalSearchClasses = [
      'global-search'
      'global-search-active' if @props.visible
    ].join(' ')

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
        />
      </div>
    </div>

module.exports = GlobalSearch
