import React from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { FetchRequests } from '../../actions/api/requests';
import { SetVisibility } from '../../actions/ui/globalSearch';

import { Typeahead } from 'react-typeahead';
import fuzzy from 'fuzzy';

class GlobalSearch extends React.Component {

  static propTypes = {
    requests: React.PropTypes.array,
    visible: React.PropTypes.bool,
    getRequests: React.PropTypes.func,
    setVisibility: React.PropTypes.func
  }

  constructor(...args) {
    super(...args);
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
        this.props.setVisibility(false);
      } else if (loadSearchKeysPressed && focusBody) {
        this.props.getRequests();
        this.props.setVisibility(true);
        event.preventDefault();
      }
    });
  }

  componentDidUpdate(prevProps) {
    if (this.props.visible && !prevProps.visible) {
      this.focus();
    }
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

  getValueFromOption(option) {
    return option.original;
  }

  optionSelected(requestIdObject) {
    const requestId = this.getValueFromOption(requestIdObject);
    app.router.navigate(`/request/${ requestId }`, { trigger: true });
    this.clear();
    this.props.setVisibility(false);
  }

  renderOption(option, index) {
    // transform fuzzy string into react component
    const bolded = option.string.map((matchInfo) => {
      if (matchInfo.match) {
        return <strong>{matchInfo.char}</strong>;
      }
      return matchInfo.char;
    });

    return <span key={index}>{bolded}</span>;
  }

  render() {
    const options = _.map(this.props.requests, (r) => r.request.id);

    const globalSearchClasses = classNames({
      'global-search': true,
      'global-search-active': this.props.visible
    });

    return (
      <div className={globalSearchClasses}>
        <div className="container">
          <div className="close-button-container">
            <a onClick={() => this.props.setVisibility(false)}>&times;</a>
          </div>

          <p className="hidden-xs text-muted tip">
            Protip: You can press <kbd>s</kbd> or <kbd>t</kbd> to open global search and <kbd>esc</kbd> to close it.
          </p>
          <Typeahead
            ref="typeahead"
            options={options}
            maxVisible={10}
            customClasses={{
              input: 'big-search-box'
            }}
            placeholder="Search all requests"
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
    getRequests: () => dispatch(FetchRequests.trigger()),
    setVisibility: (visible) => dispatch(SetVisibility(visible))
  };
}

function mapStateToProps(state) {
  return {
    requests: state.api.requests.data,
    visible: state.ui.globalSearch.visible
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(GlobalSearch);
