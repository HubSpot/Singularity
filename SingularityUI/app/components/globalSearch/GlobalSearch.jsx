import React from 'react';
import classNames from 'classnames';

import { Typeahead } from 'react-typeahead';
import fuzzy from 'fuzzy';

class GlobalSearch extends React.Component {
  constructor(...args) {
    super(...args);
    this.optionSelected = this.optionSelected.bind(this);
    this.resetSelection = this.resetSelection.bind(this);
    this.clear = this.clear.bind(this);
    this.focus = this.focus.bind(this);
    this.componentDidUpdate = this.componentDidUpdate.bind(this);
    this.render = this.render.bind(this);
  }

  optionSelected(requestIdObject) {
    const requestId = this.getValueFromOption(requestIdObject);
    app.router.navigate(`/request/${ requestId }`, { trigger: true });
    this.clear();
    return this.props.onHide();
  }

  resetSelection() {
    return this.refs.typeahead.setState({
      selectionIndex: 0
    });
  }

  clear() {
    this.refs.typeahead.setEntryText('');
    return this.resetSelection();
  }

  focus() {
    this.refs.typeahead.focus();
    return this.resetSelection();
  }

  searchOptions(inputValue, options) {
    // fuzzy lazily just appends a string before and after a matching char
    // we have to later use a simple shift-in shift-out state machine to convert
    const fuzzyOptions = {
      returnMatchInfo: true
    };

    const searched = fuzzy.filter(inputValue, options, fuzzyOptions);

    return searched;
  }

  renderOption(option, index) {
    // transform fuzzy string into react component
    const bolded = option.string.map(function(matchInfo) {
      if (matchInfo.match) {
        return <strong>{matchInfo.char}</strong>;
      } else {
        return <span>{matchInfo.char}</span>;
      }
    });

    return <span key={index}>{bolded}</span>;
  }

  getValueFromOption(option) {
    return option.original;
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.props.visible && (this.props.visible !== prevProps.visible)) {
      return this.focus();
    }
  }

  render() {
    const options = _.pluck(this.props.requests.toJSON(), 'id');

    const globalSearchClasses = classNames({
      'global-search': true,
      'global-search-active': this.props.visible
    });

    return (
      <div className={globalSearchClasses}>
        <div className='container'>
          <div className='close-button-container'>
            <a onClick={this.props.onHide}>&times;</a>
          </div>

          <p className='hidden-xs text-muted tip'>
            Protip: You can press <kbd>s</kbd> or <kbd>t</kbd> to open global search and <kbd>esc</kbd> to close it.
          </p>
          <Typeahead
            ref='typeahead'
            options={options}
            maxVisible={10}
            customClasses={{
              input: 'big-search-box'
            }}
            placeholder='Search all requests'
            onOptionSelected={this.optionSelected}
            searchOptions={this.searchOptions}
            displayOption={this.renderOption}
            formInputOption={this.getValueFromOption}
            inputDisplayOption={this.getValueFromOption}
          />
        </div>
      </div>
    );
  }
}

export default GlobalSearch;
