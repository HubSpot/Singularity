import React from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { FetchRequests } from '../../actions/api/requests';

import { Typeahead } from 'react-typeahead';
import fuzzy from 'fuzzy';

class GlobalSearch extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      visible: false
    }
    this.optionSelected = this.optionSelected.bind(this);
    this.resetSelection = this.resetSelection.bind(this);
  }

  componentWillMount() {
    this.props.getRequests();
  }

  componentDidMount() {
    window.addEventListener('keydown', (event) => {
      const focusBody = $(event.target).is('body');
      const focusInput = $(event.target).is($('input.big-search-box'));

      const modifierKey = event.metaKey || event.shiftKey || event.ctrlKey;
      // s and t
      const loadSearchKeysPressed = [83, 84].indexOf(event.keyCode) >= 0 && !modifierKey;
      const escPressed = event.keyCode === 27;

      if (escPressed && (focusBody || focusInput)) {
        this.setState({
          visible: false
        });
      } else if (loadSearchKeysPressed && focusBody) {
        this.props.getRequests();
        this.setState({
          visible: true
        });
        event.preventDefault();
      }
    });
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.state.visible && !prevState.visible) {
      this.focus();
    }
  }

  optionSelected(requestIdObject) {
    const requestId = this.getValueFromOption(requestIdObject);
    app.router.navigate(`/request/${ requestId }`, { trigger: true });
    this.clear();
    this.setState({
      visible: false
    });
  }

  resetSelection() {
    return this.refs.typeahead.setState({
      selectionIndex: 0
    });
  }

  clear() {
    this.refs.typeahead.setEntryText('');
    this.resetSelection();
  }

  focus() {
    this.refs.typeahead.focus();
    this.resetSelection();
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

  render() {
    const options = _.map(this.props.requests, (r) => r.request.id);

    const globalSearchClasses = classNames({
      'global-search': true,
      'global-search-active': this.state.visible
    });

    return (
      <div className={globalSearchClasses}>
        <div className='container'>
          <div className='close-button-container'>
            <a onClick={() => this.setState({visible: false})}>&times;</a>
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

function mapDispatchToProps(dispatch) {
  return {
    getRequests: () => dispatch(FetchRequests.trigger())
  };
}

function mapStateToProps(state) {
  return {
    requests: state.api.requests.data
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(GlobalSearch);
